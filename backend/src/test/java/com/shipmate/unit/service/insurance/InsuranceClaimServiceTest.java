package com.shipmate.unit.service.insurance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import com.shipmate.dto.request.insurance.AdminClaimDecisionRequest;
import com.shipmate.dto.request.insurance.CreateInsuranceClaimRequest;
import com.shipmate.dto.response.admin.AdminClaimResponse;
import com.shipmate.dto.response.insurance.InsuranceClaimResponse;
import com.shipmate.listener.notification.NotificationRequestedEvent;
import com.shipmate.listener.shipment.ShipmentStatusChangedEvent;
import com.shipmate.mapper.insurance.AdminClaimMapper;
import com.shipmate.mapper.insurance.InsuranceClaimMapper;
import com.shipmate.model.insuranceClaim.ClaimReason;
import com.shipmate.model.insuranceClaim.ClaimStatus;
import com.shipmate.model.insuranceClaim.InsuranceClaim;
import com.shipmate.model.notification.NotificationType;
import com.shipmate.model.payment.Payment;
import com.shipmate.model.payment.PaymentStatus;
import com.shipmate.model.shipment.Shipment;
import com.shipmate.model.shipment.ShipmentStatus;
import com.shipmate.model.user.User;
import com.shipmate.repository.insurance.InsuranceClaimRepository;
import com.shipmate.repository.payment.PaymentRepository;
import com.shipmate.repository.shipment.ShipmentRepository;
import com.shipmate.repository.user.UserRepository;
import com.shipmate.service.insurance.InsuranceClaimService;
import com.shipmate.service.payment.PaymentService;
import com.shipmate.service.photo.PhotoService;

@ExtendWith(MockitoExtension.class)
class InsuranceClaimServiceTest {

    @Mock
    private InsuranceClaimRepository claimRepository;

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentService paymentService;

    @Mock
    private InsuranceClaimMapper mapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private PhotoService photoService;

    @Mock
    private AdminClaimMapper adminClaimMapper;

    @InjectMocks
    private InsuranceClaimService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "claimWindowDays", 7L);
    }

    @Test
    void submitClaim_shouldCreateDamagedClaimWithComputedCompensation() {
        Shipment shipment = insuredShipment(ShipmentStatus.DELIVERED);
        User sender = shipment.getSender();
        CreateInsuranceClaimRequest request = new CreateInsuranceClaimRequest();
        request.setClaimReason(ClaimReason.DAMAGED);
        request.setDescription("Damaged corner");

        when(shipmentRepository.findById(shipment.getId())).thenReturn(Optional.of(shipment));
        when(userRepository.findById(sender.getId())).thenReturn(Optional.of(sender));
        when(claimRepository.existsByShipment(shipment)).thenReturn(false);
        when(claimRepository.save(any(InsuranceClaim.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponse(any(InsuranceClaim.class))).thenAnswer(invocation -> {
            InsuranceClaim claim = invocation.getArgument(0);
            return InsuranceClaimResponse.builder()
                    .shipmentId(claim.getShipment().getId())
                    .claimantId(claim.getClaimant().getId())
                    .claimReason(claim.getClaimReason())
                    .claimStatus(claim.getClaimStatus())
                    .compensationAmount(claim.getCompensationAmount())
                    .build();
        });

        InsuranceClaimResponse response = service.submitClaim(shipment.getId(), sender.getId(), request);

        assertThat(response.getClaimStatus()).isEqualTo(ClaimStatus.SUBMITTED);
        assertThat(response.getClaimReason()).isEqualTo(ClaimReason.DAMAGED);
        assertThat(response.getCompensationAmount()).isEqualByComparingTo("72.00");

        ArgumentCaptor<InsuranceClaim> captor = ArgumentCaptor.forClass(InsuranceClaim.class);
        verify(claimRepository).save(captor.capture());
        assertThat(captor.getValue().getDeductibleRate()).isEqualByComparingTo("0.10");
        assertThat(captor.getValue().getCoverageAmount()).isEqualByComparingTo("80.00");
    }

    @Test
    void submitClaim_shouldRejectExistingClaim() {
        Shipment shipment = insuredShipment(ShipmentStatus.DELIVERED);
        User sender = shipment.getSender();
        CreateInsuranceClaimRequest request = new CreateInsuranceClaimRequest();
        request.setClaimReason(ClaimReason.DAMAGED);

        when(shipmentRepository.findById(shipment.getId())).thenReturn(Optional.of(shipment));
        when(userRepository.findById(sender.getId())).thenReturn(Optional.of(sender));
        when(claimRepository.existsByShipment(shipment)).thenReturn(true);

        assertThatThrownBy(() -> service.submitClaim(shipment.getId(), sender.getId(), request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Claim already exists for this shipment");
    }

    @Test
    void submitClaim_shouldRejectUninsuredShipment() {
        Shipment shipment = insuredShipment(ShipmentStatus.DELIVERED);
        shipment.setInsuranceSelected(false);
        User sender = shipment.getSender();
        CreateInsuranceClaimRequest request = new CreateInsuranceClaimRequest();
        request.setClaimReason(ClaimReason.DAMAGED);

        when(shipmentRepository.findById(shipment.getId())).thenReturn(Optional.of(shipment));
        when(userRepository.findById(sender.getId())).thenReturn(Optional.of(sender));
        when(claimRepository.existsByShipment(shipment)).thenReturn(false);

        assertThatThrownBy(() -> service.submitClaim(shipment.getId(), sender.getId(), request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Shipment not insured");
    }

    @Test
    void submitClaim_shouldRejectExpiredClaimWindow() {
        Shipment shipment = insuredShipment(ShipmentStatus.DELIVERED);
        shipment.setDeliveredAt(Instant.now().minusSeconds(9L * 24 * 3600));
        User sender = shipment.getSender();
        CreateInsuranceClaimRequest request = new CreateInsuranceClaimRequest();
        request.setClaimReason(ClaimReason.DAMAGED);

        when(shipmentRepository.findById(shipment.getId())).thenReturn(Optional.of(shipment));
        when(userRepository.findById(sender.getId())).thenReturn(Optional.of(sender));
        when(claimRepository.existsByShipment(shipment)).thenReturn(false);

        assertThatThrownBy(() -> service.submitClaim(shipment.getId(), sender.getId(), request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Claim window expired");
    }

    @Test
    void submitClaim_shouldRejectDamagedClaimForWrongShipmentState() {
        Shipment shipment = insuredShipment(ShipmentStatus.IN_TRANSIT);
        User sender = shipment.getSender();
        CreateInsuranceClaimRequest request = new CreateInsuranceClaimRequest();
        request.setClaimReason(ClaimReason.DAMAGED);

        when(shipmentRepository.findById(shipment.getId())).thenReturn(Optional.of(shipment));
        when(userRepository.findById(sender.getId())).thenReturn(Optional.of(sender));
        when(claimRepository.existsByShipment(shipment)).thenReturn(false);

        assertThatThrownBy(() -> service.submitClaim(shipment.getId(), sender.getId(), request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("DAMAGED claim allowed only for DELIVERED shipments");
    }

    @Test
    void reviewAdminClaim_shouldApproveLostClaimAndTriggerRefundFlow() {
        Shipment shipment = insuredShipment(ShipmentStatus.IN_TRANSIT);
        User admin = user("admin@test.com");
        InsuranceClaim claim = claim(shipment, ClaimReason.LOST, ClaimStatus.SUBMITTED);
        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .shipment(shipment)
                .sender(shipment.getSender())
                .stripePaymentIntentId("pi_123")
                .amountTotal(new BigDecimal("90.00"))
                .currency("EUR")
                .paymentStatus(PaymentStatus.CAPTURED)
                .build();
        AdminClaimDecisionRequest request = new AdminClaimDecisionRequest();
        request.setDecision(ClaimStatus.APPROVED);
        request.setAdminNotes("Approved");

        when(claimRepository.findAdminById(claim.getId())).thenReturn(Optional.of(claim));
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(paymentRepository.findByShipment(shipment)).thenReturn(Optional.of(payment));
        when(adminClaimMapper.toAdminResponse(claim)).thenAnswer(invocation -> {
            InsuranceClaim mapped = invocation.getArgument(0);
            return AdminClaimResponse.builder()
                    .id(mapped.getId())
                    .claimStatus(mapped.getClaimStatus())
                    .build();
        });

        AdminClaimResponse response = service.reviewAdminClaim(claim.getId(), admin.getId(), request);

        assertThat(response.getClaimStatus()).isEqualTo(ClaimStatus.APPROVED);
        assertThat(claim.getClaimStatus()).isEqualTo(ClaimStatus.APPROVED);
        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.LOST);
        verify(paymentService).refundByAdmin(payment.getId());
        verify(shipmentRepository).save(shipment);
        verify(claimRepository).save(claim);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, org.mockito.Mockito.atLeast(2)).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getAllValues())
                .anySatisfy(event -> assertThat(event).isInstanceOf(NotificationRequestedEvent.class))
                .anySatisfy(event -> assertThat(event).isInstanceOf(ShipmentStatusChangedEvent.class));
    }

    @Test
    void reviewAdminClaim_shouldRejectClaimWithoutRefund() {
        Shipment shipment = insuredShipment(ShipmentStatus.DELIVERED);
        User admin = user("admin@test.com");
        InsuranceClaim claim = claim(shipment, ClaimReason.DAMAGED, ClaimStatus.SUBMITTED);
        AdminClaimDecisionRequest request = new AdminClaimDecisionRequest();
        request.setDecision(ClaimStatus.REJECTED);
        request.setAdminNotes("Insufficient proof");

        when(claimRepository.findAdminById(claim.getId())).thenReturn(Optional.of(claim));
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(adminClaimMapper.toAdminResponse(claim)).thenAnswer(invocation -> {
            InsuranceClaim mapped = invocation.getArgument(0);
            return AdminClaimResponse.builder()
                    .id(mapped.getId())
                    .claimStatus(mapped.getClaimStatus())
                    .build();
        });

        AdminClaimResponse response = service.reviewAdminClaim(claim.getId(), admin.getId(), request);

        assertThat(response.getClaimStatus()).isEqualTo(ClaimStatus.REJECTED);
        verify(paymentService, never()).refundByAdmin(any());

        ArgumentCaptor<NotificationRequestedEvent> captor = ArgumentCaptor.forClass(NotificationRequestedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().type()).isEqualTo(NotificationType.INSURANCE_UPDATE);
    }

    @Test
    void addClaimPhotos_shouldUploadForSubmittedClaimOwner() {
        Shipment shipment = insuredShipment(ShipmentStatus.DELIVERED);
        InsuranceClaim claim = claim(shipment, ClaimReason.DAMAGED, ClaimStatus.SUBMITTED);
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        when(claimRepository.findById(claim.getId())).thenReturn(Optional.of(claim));
        when(mapper.toResponse(claim)).thenReturn(InsuranceClaimResponse.builder()
                .id(claim.getId())
                .claimStatus(claim.getClaimStatus())
                .build());

        InsuranceClaimResponse response = service.addClaimPhotos(claim.getId(), claim.getClaimant().getId(), List.of(file));

        assertThat(response.getId()).isEqualTo(claim.getId());
        verify(photoService).uploadInsuranceClaimPhotos(claim, List.of(file));
    }

    @Test
    void addClaimPhotos_shouldRejectWrongUser() {
        Shipment shipment = insuredShipment(ShipmentStatus.DELIVERED);
        InsuranceClaim claim = claim(shipment, ClaimReason.DAMAGED, ClaimStatus.SUBMITTED);
        when(claimRepository.findById(claim.getId())).thenReturn(Optional.of(claim));

        assertThatThrownBy(() -> service.addClaimPhotos(claim.getId(), UUID.randomUUID(), List.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Not your claim");
    }

    @Test
    void listAdminClaims_shouldMapRepositoryPage() {
        Shipment shipment = insuredShipment(ShipmentStatus.DELIVERED);
        InsuranceClaim claim = claim(shipment, ClaimReason.DAMAGED, ClaimStatus.SUBMITTED);
        when(claimRepository.findByClaimStatus(ClaimStatus.SUBMITTED, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(claim)));
        when(adminClaimMapper.toAdminResponse(claim)).thenReturn(AdminClaimResponse.builder()
                .id(claim.getId())
                .claimStatus(claim.getClaimStatus())
                .build());

        var page = service.listAdminClaims(ClaimStatus.SUBMITTED, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getId()).isEqualTo(claim.getId());
    }

    @Test
    void listMyClaims_shouldMapClaimantClaims() {
        User claimant = user("sender@test.com");
        Shipment shipment = insuredShipment(ShipmentStatus.DELIVERED);
        shipment.setSender(claimant);
        InsuranceClaim claim = claim(shipment, ClaimReason.DAMAGED, ClaimStatus.SUBMITTED);

        when(userRepository.findById(claimant.getId())).thenReturn(Optional.of(claimant));
        when(claimRepository.findByClaimant(claimant)).thenReturn(List.of(claim));
        when(mapper.toResponse(claim)).thenReturn(InsuranceClaimResponse.builder()
                .id(claim.getId())
                .claimStatus(claim.getClaimStatus())
                .build());

        List<InsuranceClaimResponse> claims = service.listMyClaims(claimant.getId());

        assertThat(claims).hasSize(1);
        assertThat(claims.get(0).getId()).isEqualTo(claim.getId());
    }

    private Shipment insuredShipment(ShipmentStatus status) {
        return Shipment.builder()
                .id(UUID.randomUUID())
                .sender(user("sender@test.com"))
                .status(status)
                .insuranceSelected(true)
                .insuranceFee(new BigDecimal("10.00"))
                .declaredValue(new BigDecimal("100.00"))
                .insuranceCoverageAmount(new BigDecimal("80.00"))
                .insuranceDeductibleRate(new BigDecimal("0.10"))
                .deliveredAt(Instant.now().minusSeconds(24 * 3600))
                .updatedAt(Instant.now().minusSeconds(24 * 3600))
                .photos(Set.of())
                .build();
    }

    private InsuranceClaim claim(Shipment shipment, ClaimReason reason, ClaimStatus status) {
        return InsuranceClaim.builder()
                .id(UUID.randomUUID())
                .shipment(shipment)
                .claimant(shipment.getSender())
                .declaredValueSnapshot(new BigDecimal("100.00"))
                .coverageAmount(new BigDecimal("80.00"))
                .deductibleRate(new BigDecimal("0.10"))
                .compensationAmount(new BigDecimal("72.00"))
                .claimReason(reason)
                .claimDescription("desc")
                .claimStatus(status)
                .photos(Collections.emptySet())
                .createdAt(Instant.now())
                .build();
    }

    private User user(String email) {
        return User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .firstName("Test")
                .lastName("User")
                .build();
    }
}
