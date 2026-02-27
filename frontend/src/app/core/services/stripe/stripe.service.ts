import { Injectable } from '@angular/core';
import { loadStripe, Stripe, StripeElements } from '@stripe/stripe-js';
import { environment } from '../../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class StripeService {

  private stripePromise = loadStripe(environment.stripePublicKey);

  async getStripe(): Promise<Stripe | null> {
    return this.stripePromise;
  }

  async createElements(clientSecret: string): Promise<StripeElements | null> {
    const stripe = await this.getStripe();
    if (!stripe) return null;

    return stripe.elements({
      clientSecret,
      appearance: {
        theme: 'stripe'
      }
    });
  }
}
