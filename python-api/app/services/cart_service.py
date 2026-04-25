from typing import List, Dict, Optional
from app.services.discount_service import DiscountService

class CartItem:
    def __init__(self, id: str, name: str, price: float, quantity: int = 1):
        if price < 0:
            raise ValueError("Preço não pode ser negativo")
        self.id = id
        self.name = name
        self.price = price
        self.quantity = quantity

class CartService:
    def __init__(self, discount_service: Optional[DiscountService] = None):
        self.discount_service = discount_service or DiscountService()
        self.tax_rate = 0.08  # 8% fixo

    def calculate_cart_total(
        self, 
        items: List[Dict], 
        coupon_code: Optional[str] = None, 
        is_vip: bool = False
    ) -> Dict:
        """
        Calcula o total do carrinho, aplicando impostos e descontos.
        Estrutura de items: [{"id": "1", "name": "Prod", "price": 10.0, "quantity": 2}]
        """
        subtotal = 0.0
        processed_items = []
        
        for item_data in items:
            item = CartItem(
                id=item_data["id"],
                name=item_data["name"],
                price=item_data["price"],
                quantity=item_data.get("quantity", 1)
            )
            item_total = item.price * item.quantity
            subtotal += item_total
            processed_items.append({
                "id": item.id,
                "name": item.name,
                "total": item_total
            })

        # Aplica taxa ANTES do desconto (regra de negócio duvidosa para testar o QA)
        tax_amount = subtotal * self.tax_rate
        total_with_tax = subtotal + tax_amount

        # Calcula desconto usando o DiscountService
        # BUG PROPOSITAL: Estamos passando o total COM imposto para o calculador de desconto
        # mas o DiscountService pode ter limites baseados no preço base.
        final_price = self.discount_service.calculate_final_price(
            base_price=total_with_tax,
            discount_percentage=0.0, # Desconto base zero, depende de cupom/VIP
            coupon_code=coupon_code,
            is_vip=is_vip
        )

        # Desconto de fidelidade "hardcoded" (risco de segurança/regra de negócio)
        if subtotal > 1000 and not is_vip:
            # Dá 5% extra se for uma compra grande mas não for VIP (estranho?)
            final_price *= 0.95

        return {
            "subtotal": round(subtotal, 2),
            "tax_amount": round(tax_amount, 2),
            "final_price": round(final_price, 2),
            "items_count": len(processed_items)
        }
