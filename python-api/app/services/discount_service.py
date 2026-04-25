from typing import Optional
from decimal import Decimal


class DiscountService:
    """
    Serviço especializado em cálculo de descontos para o e-commerce.
    
    Este serviço possui regras complexas baseadas no valor da compra,
    tipo de cliente e cupons especiais.
    """

    def calculate_final_price(
        self, 
        base_price: float, 
        discount_percentage: float, 
        coupon_code: Optional[str] = None,
        is_vip: bool = False
    ) -> float:
        """
        Calcula o preço final após aplicar descontos.
        
        Regras:
        1. Desconto percentual não pode ser maior que 70% (política da empresa).
        2. Clientes VIP ganham 5% de desconto adicional fixo SOBRE o preço já descontado.
        3. Cupons 'QUERO10' dão 10 reais de desconto fixo.
        4. Cupons 'QUERO20' dão 20 reais de desconto fixo se a compra for acima de 100 reais.
        5. O preço final nunca pode ser menor que 0.
        """
        if not isinstance(base_price, (int, float)):
            raise TypeError("base_price deve ser um número")
        if not isinstance(discount_percentage, (int, float)):
            raise TypeError("discount_percentage deve ser um número")
        if coupon_code is not None and not isinstance(coupon_code, str):
            raise TypeError("coupon_code deve ser uma string")
        if not isinstance(is_vip, bool) and is_vip is not None:
             raise TypeError("is_vip deve ser um booleano")

        if base_price < 0:
            raise ValueError("Preço base não pode ser negativo")

        # Limita desconto percentual
        applied_percentage = min(discount_percentage, 70.0)
        if applied_percentage < 0:
            applied_percentage = 0.0

        discount_amount = base_price * (applied_percentage / 100.0)
        current_price = base_price - discount_amount

        # Aplica VIP
        if is_vip:
            current_price *= 0.95

        # Aplica Cupons
        if coupon_code == "QUERO10":
            current_price -= 10.0
        elif coupon_code == "QUERO20" and base_price > 100.0:
            current_price -= 20.0
        
        # Garante preço não negativo
        return max(0.0, round(current_price, 2))

    def apply_bulk_discount(self, items_count: int, total_value: float) -> float:
        """
        Aplica desconto progressivo baseado na quantidade de itens.
        5+ itens: 5%
        10+ itens: 10%
        20+ itens: 15%
        """
        if not isinstance(items_count, int):
            raise TypeError("items_count deve ser um inteiro")
        if not isinstance(total_value, (int, float)):
            raise TypeError("total_value deve ser um número")
        
        if items_count >= 20:
            factor = 0.85
        elif items_count >= 10:
            factor = 0.90
        elif items_count >= 5:
            factor = 0.95
        else:
            factor = 1.0
        
        return round(total_value * factor, 2)
