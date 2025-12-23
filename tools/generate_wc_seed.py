import datetime as dt
from dataclasses import dataclass


@dataclass(frozen=True)
class Product:
    sku: str
    name: str
    price: float  # VNĐ (nghìn) theo data hiện tại
    brand: str
    category_id: int
    image: str
    tags: list[str]
    description: str
    featured: bool = False


@dataclass(frozen=True)
class Variant:
    sku: str
    product_sku: str
    variant_name: str
    price: float
    stock: int
    color: str | None = None
    size: str | None = None  # dùng cho bộ nhớ/kích thước
    is_default: bool = False


@dataclass(frozen=True)
class Image:
    product_sku: str
    url: str
    alt: str
    is_primary: bool
    order: int


def b(v: bool) -> str:
    return "b'1'" if v else "b'0'"


def q(s: str | None) -> str:
    if s is None:
        return "NULL"
    return "'" + s.replace("\\", "\\\\").replace("'", "''") + "'"


def json_array(tags: list[str]) -> str:
    # products.tags là LONGTEXT; project đang lưu chuỗi JSON (string)
    return q(str(tags).replace("'", '"'))


def now6() -> str:
    return "NOW(6)"


def build_catalog() -> tuple[list[Product], list[Variant], list[Image]]:
    # Ảnh: dùng Unsplash CDN (ổn định). Đây là ảnh “đúng loại sản phẩm” (phone/laptop/...)
    # Nếu bạn muốn ảnh “đúng model 100%”, mình có thể đổi sang ảnh vendor CDN theo từng model.
    products: list[Product] = []
    variants: list[Variant] = []
    images: list[Image] = []

    # Phones (20)
    phones = [
        ("WC-PHO-0001", "iPhone 16 Pro Max", 34990.00, "Apple", "https://images.unsplash.com/photo-1592750475338-74b7b21085ab?w=1200&h=1200&fit=crop&auto=format&q=80", ["phones", "apple", "flagship"], True),
        ("WC-PHO-0002", "iPhone 16 Pro", 29990.00, "Apple", "https://images.unsplash.com/photo-1510557880182-3d4d3cba35a5?w=1200&h=1200&fit=crop&auto=format&q=80", ["phones", "apple", "premium"], False),
        ("WC-PHO-0003", "iPhone 16", 22990.00, "Apple", "https://images.unsplash.com/photo-1591337676887-a217a6970a8a?w=1200&h=1200&fit=crop&auto=format&q=80", ["phones", "apple"], False),
        ("WC-PHO-0011", "Samsung Galaxy S25 Ultra", 32990.00, "Samsung", "https://images.unsplash.com/photo-1610945265064-0e34e5519bbf?w=1200&h=1200&fit=crop&auto=format&q=80", ["phones", "samsung", "flagship"], True),
        ("WC-PHO-0012", "Samsung Galaxy S25+", 26990.00, "Samsung", "https://images.unsplash.com/photo-1565849904461-04a58ad377e0?w=1200&h=1200&fit=crop&auto=format&q=80", ["phones", "samsung"], False),
        ("WC-PHO-0021", "Google Pixel 9 Pro", 25990.00, "Google", "https://images.unsplash.com/photo-1598327105666-5b89351aff97?w=1200&h=1200&fit=crop&auto=format&q=80", ["phones", "google", "camera"], True),
        ("WC-PHO-0022", "Google Pixel 9", 20990.00, "Google", "https://images.unsplash.com/photo-1512054502232-10a0a035d672?w=1200&h=1200&fit=crop&auto=format&q=80", ["phones", "google"], False),
        ("WC-PHO-0031", "Xiaomi 15 Ultra", 27990.00, "Xiaomi", "https://images.unsplash.com/photo-1585060544812-6b45742d762f?w=1200&h=1200&fit=crop&auto=format&q=80", ["phones", "xiaomi", "camera"], True),
        ("WC-PHO-0032", "Xiaomi 15 Pro", 21990.00, "Xiaomi", "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=1200&h=1200&fit=crop&auto=format&q=80", ["phones", "xiaomi"], False),
        ("WC-PHO-0041", "OnePlus 13", 19990.00, "OnePlus", "https://images.unsplash.com/photo-1574944985070-8f3ebc6b79d2?w=1200&h=1200&fit=crop&auto=format&q=80", ["phones", "oneplus", "performance"], False),
        ("WC-PHO-0051", "Nothing Phone (3)", 16990.00, "Nothing", "https://images.unsplash.com/photo-1580910051074-3eb694886f2b?w=1200&h=1200&fit=crop&auto=format&q=80", ["phones", "nothing", "design"], False),
        ("WC-PHO-0061", "Motorola Edge 50 Pro", 15990.00, "Motorola", "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=1200&h=1200&fit=crop&auto=format&q=80", ["phones", "motorola"], False),
        ("WC-PHO-0071", "Huawei Pura 70 Pro", 23990.00, "Huawei", "https://images.unsplash.com/photo-1592899677977-9c10ca588bbd?w=1200&h=1200&fit=crop&auto=format&q=80", ["phones", "huawei", "camera"], False),
        ("WC-PHO-0081", "ASUS ROG Phone 8 Pro", 29990.00, "ASUS", "https://images.unsplash.com/photo-1605236453806-6ff36851218e?w=1200&h=1200&fit=crop&auto=format&q=80", ["phones", "asus", "gaming"], False),
        ("WC-PHO-0091", "Samsung Galaxy Z Fold6", 42990.00, "Samsung", "https://images.unsplash.com/photo-1523206489230-c012c64b2b48?w=1200&h=1200&fit=crop&auto=format&q=80", ["phones", "samsung", "foldable"], True),
        ("WC-PHO-0092", "Samsung Galaxy Z Flip6", 25990.00, "Samsung", "https://images.unsplash.com/photo-1523206489230-c012c64b2b48?w=1200&h=1200&fit=crop&auto=format&q=80", ["phones", "samsung", "foldable"], False),
        ("WC-PHO-0101", "iPhone 15 Pro Max", 31990.00, "Apple", "https://images.unsplash.com/photo-1592750475338-74b7b21085ab?w=1200&h=1200&fit=crop&auto=format&q=80", ["phones", "apple"], False),
        ("WC-PHO-0102", "Samsung Galaxy S24 Ultra", 31990.00, "Samsung", "https://images.unsplash.com/photo-1610945265064-0e34e5519bbf?w=1200&h=1200&fit=crop&auto=format&q=80", ["phones", "samsung"], False),
        ("WC-PHO-0111", "Google Pixel 8 Pro", 19990.00, "Google", "https://images.unsplash.com/photo-1598327105666-5b89351aff97?w=1200&h=1200&fit=crop&auto=format&q=80", ["phones", "google"], False),
        ("WC-PHO-0121", "OnePlus 12", 17990.00, "OnePlus", "https://images.unsplash.com/photo-1574944985070-8f3ebc6b79d2?w=1200&h=1200&fit=crop&auto=format&q=80", ["phones", "oneplus"], False),
    ]
    for sku, name, price, brand, img, tags, featured in phones:
        products.append(
            Product(
                sku=sku,
                name=name,
                price=price,
                brand=brand,
                category_id=2,
                image=img,
                tags=tags,
                description=f"{name}: {', '.join(tags)}. Sản phẩm chính hãng, bảo hành uy tín.",
                featured=featured,
            )
        )

    # Variants for phones (màu + bộ nhớ; mỗi variant có giá khác nhau)
    def add_phone_variants(
        product_sku: str,
        base_name: str,
        base_price: float,
        mem_tiers: list[tuple[str, float]],
        colors: list[tuple[str, float]],  # (color, price_delta)
        base_stock: int = 30,
    ):
        for mi, (mem, mem_add) in enumerate(mem_tiers):
            for ci, (color, color_add) in enumerate(colors):
                is_def = (mi == 0 and ci == 0)
                # keep sku short & unique
                mem_code = mem.replace("GB", "").replace("TB", "T").replace("/", "-").replace(" ", "")
                col_code = "".join([c for c in color.upper() if c.isalnum()])[:4]
                vsku = f"{product_sku}-{mem_code}-{col_code}"
                variants.append(
                    Variant(
                        sku=vsku,
                        product_sku=product_sku,
                        variant_name=f"{base_name} {mem} {color}",
                        price=round(base_price + mem_add + color_add, 2),
                        stock=max(0, base_stock - mi * 4 - ci * 2),
                        color=color,
                        size=mem,
                        is_default=is_def,
                    )
                )

    # Define tier rules (realistic deltas in "VNĐ nghìn")
    premium_mems = [("256GB", 0.00), ("512GB", 3000.00), ("1TB", 8000.00)]
    mid_mems = [("128GB", 0.00), ("256GB", 1500.00), ("512GB", 4500.00)]
    fold_mems = [("256GB", 0.00), ("512GB", 4000.00), ("1TB", 9000.00)]

    apple_colors = [("Black", 0.00), ("Silver", 100.00), ("Blue", 150.00), ("Gold", 200.00)]
    samsung_colors = [("Black", 0.00), ("Silver", 100.00), ("Green", 150.00), ("Purple", 200.00)]
    google_colors = [("Obsidian", 0.00), ("Porcelain", 100.00), ("Bay", 150.00)]
    xiaomi_colors = [("Black", 0.00), ("White", 80.00), ("Green", 120.00)]
    oneplus_colors = [("Black", 0.00), ("Green", 80.00)]
    nothing_colors = [("Black", 0.00), ("White", 50.00)]

    # Apply variants broadly for all phone products in this catalog
    for p in [pp for pp in products if pp.category_id == 2]:
        if "Fold" in p.name or "Flip" in p.name:
            add_phone_variants(p.sku, p.name, p.price, fold_mems, samsung_colors, base_stock=18)
        elif "Ultra" in p.name or "Pro Max" in p.name or "Ultra" in p.tags:
            add_phone_variants(p.sku, p.name, p.price, premium_mems, (apple_colors if p.brand == "Apple" else samsung_colors), base_stock=22)
        elif "Pro" in p.name:
            add_phone_variants(
                p.sku,
                p.name,
                p.price,
                [("256GB", 0.00), ("512GB", 3000.00)],
                apple_colors if p.brand == "Apple" else samsung_colors,
                base_stock=24,
            )
        else:
            colors = {
                "Apple": apple_colors,
                "Samsung": samsung_colors,
                "Google": google_colors,
                "Xiaomi": xiaomi_colors,
                "OnePlus": oneplus_colors,
                "Nothing": nothing_colors,
                "Motorola": [("Black", 0.00), ("Blue", 50.00)],
                "Huawei": [("Black", 0.00), ("Green", 80.00)],
                "ASUS": [("Black", 0.00), ("Red", 150.00)],
            }.get(p.brand, [("Black", 0.00), ("White", 50.00)])
            add_phone_variants(p.sku, p.name, p.price, mid_mems, colors, base_stock=26)

    # Laptops (15)
    laptops = [
        ("WC-LAP-0001", "MacBook Air (M4) 13-inch", 28990.00, "Apple", "https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=1200&h=800&fit=crop&auto=format&q=80", ["laptops", "apple", "ultrabook"], True),
        ("WC-LAP-0002", "MacBook Pro 14-inch (M4 Pro)", 52990.00, "Apple", "https://images.unsplash.com/photo-1611186871348-b1ce696e52c9?w=1200&h=800&fit=crop&auto=format&q=80", ["laptops", "apple", "creator"], True),
        ("WC-LAP-0011", "Dell XPS 15", 45990.00, "Dell", "https://images.unsplash.com/photo-1593642632559-0c6d3fc62b89?w=1200&h=800&fit=crop&auto=format&q=80", ["laptops", "dell", "premium"], False),
        ("WC-LAP-0012", "Dell XPS 13 Plus", 38990.00, "Dell", "https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=1200&h=800&fit=crop&auto=format&q=80", ["laptops", "dell", "ultrabook"], False),
        ("WC-LAP-0021", "Lenovo ThinkPad X1 Carbon", 42990.00, "Lenovo", "https://images.unsplash.com/photo-1584925181683-0a9f0d5b6e9e?w=1200&h=800&fit=crop&auto=format&q=80", ["laptops", "lenovo", "business"], False),
        ("WC-LAP-0031", "ASUS ZenBook 14 OLED", 28990.00, "ASUS", "https://images.unsplash.com/photo-1541807084-5c52b6b3adef?w=1200&h=800&fit=crop&auto=format&q=80", ["laptops", "asus", "oled"], False),
        ("WC-LAP-0041", "Razer Blade 16", 69990.00, "Razer", "https://images.unsplash.com/photo-1588872657578-7efd1f1555ed?w=1200&h=800&fit=crop&auto=format&q=80", ["laptops", "razer", "gaming"], True),
        ("WC-LAP-0051", "MSI Stealth 16 Studio", 48990.00, "MSI", "https://images.unsplash.com/photo-1593642702821-c8da6771f0c6?w=1200&h=800&fit=crop&auto=format&q=80", ["laptops", "msi", "gaming"], False),
        ("WC-LAP-0061", "HP Spectre x360 14", 39990.00, "HP", "https://images.unsplash.com/photo-1525547719571-a2d4ac8945e2?w=1200&h=800&fit=crop&auto=format&q=80", ["laptops", "hp", "premium"], False),
        ("WC-LAP-0071", "LG Gram 17", 35990.00, "LG", "https://images.unsplash.com/photo-1525547719571-a2d4ac8945e2?w=1200&h=800&fit=crop&auto=format&q=80", ["laptops", "lg", "lightweight"], False),
        ("WC-LAP-0081", "Lenovo Legion 7", 55990.00, "Lenovo", "https://images.unsplash.com/photo-1603302576837-37561b2e2302?w=1200&h=800&fit=crop&auto=format&q=80", ["laptops", "lenovo", "gaming"], False),
        ("WC-LAP-0091", "ASUS ROG Zephyrus G16", 59990.00, "ASUS", "https://images.unsplash.com/photo-1593642702821-c8da6771f0c6?w=1200&h=800&fit=crop&auto=format&q=80", ["laptops", "asus", "gaming"], False),
        ("WC-LAP-0101", "Dell Inspiron 14 Plus", 23990.00, "Dell", "https://images.unsplash.com/photo-1541807084-5c52b6b3adef?w=1200&h=800&fit=crop&auto=format&q=80", ["laptops", "dell"], False),
        ("WC-LAP-0111", "HP Envy 15", 28990.00, "HP", "https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=1200&h=800&fit=crop&auto=format&q=80", ["laptops", "hp"], False),
        ("WC-LAP-0121", "MacBook Pro 16-inch (M4 Max)", 79990.00, "Apple", "https://images.unsplash.com/photo-1611186871348-b1ce696e52c9?w=1200&h=800&fit=crop&auto=format&q=80", ["laptops", "apple", "creator"], True),
    ]
    for sku, name, price, brand, img, tags, featured in laptops:
        products.append(
            Product(
                sku=sku,
                name=name,
                price=price,
                brand=brand,
                category_id=1,
                image=img,
                tags=tags,
                description=f"{name}: laptop cao cấp, hiệu năng ổn định, build chất lượng.",
                featured=featured,
            )
        )

    # Laptop variants (RAM/SSD)
    def add_lap_variants(product_sku: str, base_name: str, base_price: float):
        configs = [
            ("16GB/512GB", 0.00),
            ("16GB/1TB", 2500.00),
            ("32GB/1TB", 6500.00),
            ("32GB/2TB", 11500.00),
        ]
        colors = ["Silver", "Space Gray", "Black", "Midnight"]
        for ci, (cfg, add) in enumerate(configs):
            for cj, color in enumerate(colors):
                is_def = (ci == 0 and cj == 0)
                vsku = f"{product_sku}-{cfg.replace('/','-')}-{color.replace(' ','')[:3].upper()}"
                variants.append(
                    Variant(
                        sku=vsku,
                        product_sku=product_sku,
                        variant_name=f"{base_name} {cfg} {color}",
                        # slight premium for colors too
                        price=round(base_price + add + (150.0 if color in ("Midnight", "Black") else 0.0), 2),
                        stock=max(0, 18 - ci * 3 - cj),
                        color=color,
                        size=cfg,
                        is_default=is_def,
                    )
                )

    # Apply variants for ALL laptops (RAM/SSD/Color) => mỗi variant giá khác nhau
    for lp in [pp for pp in products if pp.category_id == 1]:
        add_lap_variants(lp.sku, lp.name, lp.price)

    # Headphones (15)
    headphones = [
        ("WC-HEA-0001", "Sony WH-1000XM5", 8490.00, "Sony", "https://images.unsplash.com/photo-1618366712010-f4ae9c647dcb?w=1200&h=1200&fit=crop&auto=format&q=80", ["headphones", "sony", "anc"], True),
        ("WC-HEA-0002", "Sony WF-1000XM5", 5990.00, "Sony", "https://images.unsplash.com/photo-1590658268037-6bf12165a8df?w=1200&h=1200&fit=crop&auto=format&q=80", ["headphones", "sony", "earbuds"], False),
        ("WC-HEA-0011", "Bose QuietComfort Ultra", 9990.00, "Bose", "https://images.unsplash.com/photo-1546435770-a3e426bf472b?w=1200&h=1200&fit=crop&auto=format&q=80", ["headphones", "bose", "anc"], True),
        ("WC-HEA-0021", "Sennheiser Momentum 4", 8990.00, "Sennheiser", "https://images.unsplash.com/photo-1484704849700-f032a568e944?w=1200&h=1200&fit=crop&auto=format&q=80", ["headphones", "sennheiser"], False),
        ("WC-HEA-0031", "Apple AirPods Pro 2 (USB‑C)", 6290.00, "Apple", "https://images.unsplash.com/photo-1600294037681-c80b4cb5b434?w=1200&h=1200&fit=crop&auto=format&q=80", ["headphones", "apple", "earbuds"], False),
        ("WC-HEA-0032", "Apple AirPods Max", 13990.00, "Apple", "https://images.unsplash.com/photo-1613040809024-b4ef7ba99bc3?w=1200&h=1200&fit=crop&auto=format&q=80", ["headphones", "apple", "premium"], True),
        ("WC-HEA-0041", "JBL Tour One M2", 7490.00, "JBL", "https://images.unsplash.com/photo-1583394838336-acd977736f90?w=1200&h=1200&fit=crop&auto=format&q=80", ["headphones", "jbl"], False),
        ("WC-HEA-0051", "Samsung Galaxy Buds3 Pro", 5490.00, "Samsung", "https://images.unsplash.com/photo-1606220588913-b3aacb4d2f46?w=1200&h=1200&fit=crop&auto=format&q=80", ["headphones", "samsung", "earbuds"], False),
        ("WC-HEA-0061", "Nothing Ear (a)", 2490.00, "Nothing", "https://images.unsplash.com/photo-1600294037681-c80b4cb5b434?w=1200&h=1200&fit=crop&auto=format&q=80", ["headphones", "nothing", "earbuds"], False),
        ("WC-HEA-0071", "Anker Soundcore Liberty 4", 3290.00, "Anker", "https://images.unsplash.com/photo-1590658268037-6bf12165a8df?w=1200&h=1200&fit=crop&auto=format&q=80", ["headphones", "anker", "earbuds"], False),
        ("WC-HEA-0081", "Bang & Olufsen Beoplay H95", 19990.00, "Bose", "https://images.unsplash.com/photo-1545127398-14699f92334b?w=1200&h=1200&fit=crop&auto=format&q=80", ["headphones", "luxury"], True),
        ("WC-HEA-0091", "Audio-Technica ATH-M50xBT2", 5490.00, "Sony", "https://images.unsplash.com/photo-1529725533898-5fe23a5a5c87?w=1200&h=1200&fit=crop&auto=format&q=80", ["headphones", "studio"], False),
        ("WC-HEA-0101", "Jabra Elite 10", 4990.00, "Samsung", "https://images.unsplash.com/photo-1544303860153-4b5b77f58c6f?w=1200&h=1200&fit=crop&auto=format&q=80", ["headphones", "earbuds"], False),
        ("WC-HEA-0111", "Bose QuietComfort Ultra Earbuds", 6990.00, "Bose", "https://images.unsplash.com/photo-1606220588913-b3aacb4d2f46?w=1200&h=1200&fit=crop&auto=format&q=80", ["headphones", "bose", "earbuds"], False),
        ("WC-HEA-0121", "Sony INZONE H9", 6990.00, "Sony", "https://images.unsplash.com/photo-1546435770-a3e426bf472b?w=1200&h=1200&fit=crop&auto=format&q=80", ["headphones", "gaming"], False),
    ]
    for sku, name, price, brand, img, tags, featured in headphones:
        products.append(
            Product(
                sku=sku,
                name=name,
                price=price,
                brand=brand,
                category_id=3,
                image=img,
                tags=tags,
                description=f"{name}: âm thanh chất lượng, thiết kế đẹp, trải nghiệm cao cấp.",
                featured=featured,
            )
        )

    # Headphone variants (color with slight price deltas so each variant price differs)
    for ps, base, price, cols in [
        ("WC-HEA-0001", "Sony WH-1000XM5", 8490.00, [("Black", 0.0), ("Silver", 100.0), ("Blue", 150.0), ("Sandstone", 200.0)]),
        ("WC-HEA-0011", "Bose QuietComfort Ultra", 9990.00, [("Black", 0.0), ("White Smoke", 100.0), ("Sandstone", 150.0)]),
        ("WC-HEA-0032", "Apple AirPods Max", 13990.00, [("Black", 0.0), ("Silver", 150.0), ("Blue", 200.0), ("Pink", 250.0), ("Green", 300.0)]),
    ]:
        for i, (color, add) in enumerate(cols):
            variants.append(
                Variant(
                    sku=f"{ps}-{''.join([c for c in color.upper() if c.isalnum()])[:4]}",
                    product_sku=ps,
                    variant_name=f"{base} {color}",
                    price=round(price + add, 2),
                    stock=max(0, 30 - i * 3),
                    color=color,
                    size="One Size",
                    is_default=(i == 0),
                )
            )

    # Smartwatch (10)
    watches = [
        ("WC-SMA-0001", "Apple Watch Series 10", 12990.00, "Apple", "https://images.unsplash.com/photo-1551816230-ef5deaed4a26?w=1200&h=1200&fit=crop&auto=format&q=80", ["smartwatch", "apple"], True),
        ("WC-SMA-0002", "Apple Watch Ultra 2", 20990.00, "Apple", "https://images.unsplash.com/photo-1579586337278-3befd40fd17a?w=1200&h=1200&fit=crop&auto=format&q=80", ["smartwatch", "apple", "outdoor"], True),
        ("WC-SMA-0011", "Samsung Galaxy Watch 7", 7990.00, "Samsung", "https://images.unsplash.com/photo-1434493789847-2f02dc6ca35d?w=1200&h=1200&fit=crop&auto=format&q=80", ["smartwatch", "samsung"], False),
        ("WC-SMA-0021", "Garmin Fenix 8", 23990.00, "Garmin", "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=1200&h=1200&fit=crop&auto=format&q=80", ["smartwatch", "garmin"], False),
        ("WC-SMA-0031", "Fitbit Sense 2", 7490.00, "Fitbit", "https://images.unsplash.com/photo-1510017803434-a899398421b3?w=1200&h=1200&fit=crop&auto=format&q=80", ["smartwatch", "fitbit"], False),
        ("WC-SMA-0041", "Amazfit GTR 4", 4990.00, "Amazfit", "https://images.unsplash.com/photo-1508685096489-7aacd43bd3b1?w=1200&h=1200&fit=crop&auto=format&q=80", ["smartwatch", "amazfit"], False),
        ("WC-SMA-0051", "Google Pixel Watch 3", 8990.00, "Google", "https://images.unsplash.com/photo-1557438159-51eec7a6c9e8?w=1200&h=1200&fit=crop&auto=format&q=80", ["smartwatch", "google"], False),
        ("WC-SMA-0061", "Garmin Venu 3", 11990.00, "Garmin", "https://images.unsplash.com/photo-1544117519-31a4b719223d?w=1200&h=1200&fit=crop&auto=format&q=80", ["smartwatch", "garmin"], False),
        ("WC-SMA-0071", "Samsung Galaxy Watch 6 Classic", 8990.00, "Samsung", "https://images.unsplash.com/photo-1434493789847-2f02dc6ca35d?w=1200&h=1200&fit=crop&auto=format&q=80", ["smartwatch", "samsung"], False),
        ("WC-SMA-0081", "Withings ScanWatch 2", 8990.00, "Google", "https://images.unsplash.com/photo-1557438159-51eec7a6c9e8?w=1200&h=1200&fit=crop&auto=format&q=80", ["smartwatch", "health"], False),
    ]
    for sku, name, price, brand, img, tags, featured in watches:
        products.append(
            Product(
                sku=sku,
                name=name,
                price=price,
                brand=brand,
                category_id=5,
                image=img,
                tags=tags,
                description=f"{name}: theo dõi sức khoẻ, thông báo, luyện tập, pin ổn.",
                featured=featured,
            )
        )

    # Watch variants (size + gps)
    def add_watch_variants(product_sku: str, base_name: str, base_price: float):
        sizes = ["41mm", "45mm"]
        conns = [("GPS", 0.00), ("GPS+Cellular", 2000.00)]
        colors = ["Midnight", "Starlight", "Silver"]
        for si, size in enumerate(sizes):
            for ci, (conn, add) in enumerate(conns):
                for col_i, color in enumerate(colors):
                    is_def = (si == 0 and ci == 0 and col_i == 0)
                    # IMPORTANT: make sku unique for GPS vs GPS+Cellular (previously both became GPS)
                    conn_code = "GPS" if conn == "GPS" else "CEL"
                    vsku = f"{product_sku}-{size}-{conn_code}-{color[:3].upper()}"
                    variants.append(
                        Variant(
                            sku=vsku,
                            product_sku=product_sku,
                            variant_name=f"{base_name} {size} {conn} {color}",
                            price=round(base_price + add + (500.0 if size == '45mm' else 0.0), 2),
                            stock=25 - si * 3 - ci * 2 - col_i,
                            color=color,
                            size=f"{size} {conn}",
                            is_default=is_def,
                        )
                    )

    add_watch_variants("WC-SMA-0001", "Apple Watch Series 10", 12990.00)
    add_watch_variants("WC-SMA-0011", "Samsung Galaxy Watch 7", 7990.00)

    # Cameras (12)
    cams = [
        ("WC-CAM-0001", "Sony A7 IV", 58990.00, "Sony", "https://images.unsplash.com/photo-1502920917128-1aa500764cbd?w=1200&h=1200&fit=crop&auto=format&q=80", ["camera", "sony", "mirrorless"], True),
        ("WC-CAM-0002", "Canon EOS R6 Mark II", 62990.00, "Canon", "https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=1200&h=1200&fit=crop&auto=format&q=80", ["camera", "canon", "mirrorless"], True),
        ("WC-CAM-0011", "Fujifilm X-T5", 42990.00, "Fujifilm", "https://images.unsplash.com/photo-1606986628120-6896005e0c95?w=1200&h=1200&fit=crop&auto=format&q=80", ["camera", "fujifilm"], False),
        ("WC-CAM-0021", "DJI Mini 4 Pro", 22990.00, "DJI", "https://images.unsplash.com/photo-1507582020474-9a35b7d455d9?w=1200&h=1200&fit=crop&auto=format&q=80", ["camera", "dji", "drone"], True),
        ("WC-CAM-0031", "GoPro HERO 12 Black", 12990.00, "GoPro", "https://images.unsplash.com/photo-1564466809058-bf4114d55352?w=1200&h=1200&fit=crop&auto=format&q=80", ["camera", "gopro"], False),
        ("WC-CAM-0041", "Insta360 X4", 13990.00, "Insta360", "https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f?w=1200&h=1200&fit=crop&auto=format&q=80", ["camera", "insta360"], False),
        ("WC-CAM-0051", "DJI Osmo Action 4", 9990.00, "DJI", "https://images.unsplash.com/photo-1564466809058-bf4114d55352?w=1200&h=1200&fit=crop&auto=format&q=80", ["camera", "dji"], False),
        ("WC-CAM-0061", "Canon EOS R8", 45990.00, "Canon", "https://images.unsplash.com/photo-1502982720700-bfff97f2ecac?w=1200&h=1200&fit=crop&auto=format&q=80", ["camera", "canon"], False),
        ("WC-CAM-0071", "Sony ZV-E10 II", 22990.00, "Sony", "https://images.unsplash.com/photo-1510127034890-ba27508e9f1c?w=1200&h=1200&fit=crop&auto=format&q=80", ["camera", "sony", "vlog"], False),
        ("WC-CAM-0081", "DJI Air 3", 27990.00, "DJI", "https://images.unsplash.com/photo-1473968512647-3e447244af8f?w=1200&h=1200&fit=crop&auto=format&q=80", ["camera", "dji", "drone"], False),
        ("WC-CAM-0091", "Fujifilm X100VI", 43990.00, "Fujifilm", "https://images.unsplash.com/photo-1502982720700-bfff97f2ecac?w=1200&h=1200&fit=crop&auto=format&q=80", ["camera", "fujifilm"], True),
        ("WC-CAM-0101", "Sony FX30", 41990.00, "Sony", "https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=1200&h=1200&fit=crop&auto=format&q=80", ["camera", "sony", "cinema"], False),
    ]
    for sku, name, price, brand, img, tags, featured in cams:
        products.append(
            Product(
                sku=sku,
                name=name,
                price=price,
                brand=brand,
                category_id=6,
                image=img,
                tags=tags,
                description=f"{name}: thiết bị quay/chụp chất lượng cao, phù hợp creator & pro.",
                featured=featured,
            )
        )

    # Camera variants (body/kit/bundle)
    def add_camera_variants(product_sku: str, base_name: str, base_price: float):
        packs = [
            ("Body Only", 0.00),
            ("Kit 24-70mm", 8000.00),
            ("Creator Bundle", 12000.00),
        ]
        for i, (pack, add) in enumerate(packs):
            variants.append(
                Variant(
                    sku=f"{product_sku}-PK{i+1}",
                    product_sku=product_sku,
                    variant_name=f"{base_name} - {pack}",
                    price=round(base_price + add, 2),
                    stock=10 - i * 2,
                    color="Black",
                    size=pack,
                    is_default=(i == 0),
                )
            )

    add_camera_variants("WC-CAM-0001", "Sony A7 IV", 58990.00)
    add_camera_variants("WC-CAM-0002", "Canon EOS R6 Mark II", 62990.00)
    add_camera_variants("WC-CAM-0011", "Fujifilm X-T5", 42990.00)

    # Multiple images (6 per product)
    img_pool = {
        1: [
            "https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=1200&h=800&fit=crop&auto=format&q=80",
            "https://images.unsplash.com/photo-1611186871348-b1ce696e52c9?w=1200&h=800&fit=crop&auto=format&q=80",
            "https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=1200&h=800&fit=crop&auto=format&q=80",
            "https://images.unsplash.com/photo-1541807084-5c52b6b3adef?w=1200&h=800&fit=crop&auto=format&q=80",
            "https://images.unsplash.com/photo-1593642702821-c8da6771f0c6?w=1200&h=800&fit=crop&auto=format&q=80",
            "https://images.unsplash.com/photo-1603302576837-37561b2e2302?w=1200&h=800&fit=crop&auto=format&q=80",
        ],
        2: [
            "https://images.unsplash.com/photo-1592750475338-74b7b21085ab?w=1200&h=1200&fit=crop&auto=format&q=80",
            "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=1200&h=1200&fit=crop&auto=format&q=80",
            "https://images.unsplash.com/photo-1598327105666-5b89351aff97?w=1200&h=1200&fit=crop&auto=format&q=80",
            "https://images.unsplash.com/photo-1585060544812-6b45742d762f?w=1200&h=1200&fit=crop&auto=format&q=80",
            "https://images.unsplash.com/photo-1510557880182-3d4d3cba35a5?w=1200&h=1200&fit=crop&auto=format&q=80",
            "https://images.unsplash.com/photo-1565849904461-04a58ad377e0?w=1200&h=1200&fit=crop&auto=format&q=80",
        ],
        3: [
            "https://images.unsplash.com/photo-1618366712010-f4ae9c647dcb?w=1200&h=1200&fit=crop&auto=format&q=80",
            "https://images.unsplash.com/photo-1546435770-a3e426bf472b?w=1200&h=1200&fit=crop&auto=format&q=80",
            "https://images.unsplash.com/photo-1484704849700-f032a568e944?w=1200&h=1200&fit=crop&auto=format&q=80",
            "https://images.unsplash.com/photo-1600294037681-c80b4cb5b434?w=1200&h=1200&fit=crop&auto=format&q=80",
            "https://images.unsplash.com/photo-1545127398-14699f92334b?w=1200&h=1200&fit=crop&auto=format&q=80",
            "https://images.unsplash.com/photo-1583394838336-acd977736f90?w=1200&h=1200&fit=crop&auto=format&q=80",
        ],
        5: [
            "https://images.unsplash.com/photo-1551816230-ef5deaed4a26?w=1200&h=1200&fit=crop&auto=format&q=80",
            "https://images.unsplash.com/photo-1434493789847-2f02dc6ca35d?w=1200&h=1200&fit=crop&auto=format&q=80",
            "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=1200&h=1200&fit=crop&auto=format&q=80",
            "https://images.unsplash.com/photo-1508685096489-7aacd43bd3b1?w=1200&h=1200&fit=crop&auto=format&q=80",
            "https://images.unsplash.com/photo-1557438159-51eec7a6c9e8?w=1200&h=1200&fit=crop&auto=format&q=80",
            "https://images.unsplash.com/photo-1544117519-31a4b719223d?w=1200&h=1200&fit=crop&auto=format&q=80",
        ],
        6: [
            "https://images.unsplash.com/photo-1502920917128-1aa500764cbd?w=1200&h=1200&fit=crop&auto=format&q=80",
            "https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=1200&h=1200&fit=crop&auto=format&q=80",
            "https://images.unsplash.com/photo-1606986628120-6896005e0c95?w=1200&h=1200&fit=crop&auto=format&q=80",
            "https://images.unsplash.com/photo-1507582020474-9a35b7d455d9?w=1200&h=1200&fit=crop&auto=format&q=80",
            "https://images.unsplash.com/photo-1473968512647-3e447244af8f?w=1200&h=1200&fit=crop&auto=format&q=80",
            "https://images.unsplash.com/photo-1564466809058-bf4114d55352?w=1200&h=1200&fit=crop&auto=format&q=80",
        ],
    }
    for p in products:
        urls = img_pool.get(p.category_id, [p.image])[:6]
        for idx, url in enumerate(urls):
            images.append(
                Image(
                    product_sku=p.sku,
                    url=url,
                    alt=f"{p.name} image {idx+1}",
                    is_primary=(idx == 0),
                    order=idx,
                )
            )

    return products, variants, images


def generate_sql(products: list[Product], variants: list[Variant], images: list[Image]) -> str:
    lines: list[str] = []
    lines.append("-- =====================================================")
    lines.append("-- WORLD-CLASS SEED DATASET (WC-*)")
    lines.append("-- - Uses real MySQL schema from product_db")
    lines.append("-- - Upserts by SKU (products.sku unique, product_variants.sku unique)")
    lines.append("-- - Refreshes product_images for WC-* products")
    lines.append("-- =====================================================")
    lines.append("")
    lines.append("SET SQL_SAFE_UPDATES = 0;")
    lines.append("USE product_db;")
    lines.append("")
    lines.append("-- Ensure missing brands exist (INSERT IGNORE)")
    brand_names = sorted(set([p.brand for p in products]))
    for bn in brand_names:
        lines.append(f"INSERT IGNORE INTO brands (name, description) VALUES ({q(bn)}, {q(bn)});")
    lines.append("")
    lines.append("-- Clear images for WC-* products to avoid duplicates")
    lines.append("DELETE FROM product_images")
    lines.append("WHERE product_id COLLATE utf8mb4_unicode_ci IN (")
    lines.append("  SELECT id COLLATE utf8mb4_unicode_ci FROM products WHERE sku LIKE 'WC-%'")
    lines.append(");")
    lines.append("")
    lines.append("-- Upsert products (keep stable id by reusing existing id when sku matches)")
    lines.append("DROP TEMPORARY TABLE IF EXISTS wc_products;")
    lines.append("CREATE TEMPORARY TABLE wc_products (")
    lines.append("  sku VARCHAR(50) NOT NULL,")
    lines.append("  name VARCHAR(200) NOT NULL,")
    lines.append("  price DECIMAL(10,2) NOT NULL,")
    lines.append("  brand_name VARCHAR(255) NOT NULL,")
    lines.append("  category_id BIGINT NOT NULL,")
    lines.append("  image_url VARCHAR(255) NULL,")
    lines.append("  description LONGTEXT NOT NULL,")
    lines.append("  is_featured BIT(1) NOT NULL,")
    lines.append("  tags LONGTEXT NULL")
    lines.append(") ENGINE=InnoDB;")
    for p in products:
        tags_json = q(str(p.tags).replace("'", '"'))
        lines.append(
            "INSERT INTO wc_products (sku,name,price,brand_name,category_id,image_url,description,is_featured,tags) VALUES "
            f"({q(p.sku)},{q(p.name)},{p.price:.2f},{q(p.brand)},{p.category_id},{q(p.image)},{q(p.description)},{b(p.featured)},{tags_json});"
        )
    lines.append("")
    lines.append("INSERT INTO products (")
    lines.append("  id, sku, name, price, brand_id, category_id, image_url, description,")
    lines.append("  is_active, is_featured, is_on_sale, is_deleted, stock_quantity, low_stock_threshold, requires_shipping,")
    lines.append("  seo_title, seo_description, tags, created_at, updated_at")
    lines.append(")")
    lines.append("SELECT")
    lines.append("  COALESCE(p.id, UUID()) AS id,")
    lines.append("  s.sku, s.name, s.price, b.id AS brand_id, s.category_id, s.image_url, s.description,")
    lines.append("  b'1' AS is_active, s.is_featured AS is_featured, b'0' AS is_on_sale, b'0' AS is_deleted,")
    lines.append("  50 AS stock_quantity, 10 AS low_stock_threshold, b'1' AS requires_shipping,")
    lines.append("  CONCAT(s.name, ' - Chính hãng | TechHub') AS seo_title,")
    lines.append("  CONCAT('Mua ', s.name, ' chính hãng tại TechHub. Giao hàng nhanh, bảo hành uy tín.') AS seo_description,")
    lines.append("  s.tags AS tags,")
    lines.append("  NOW(6) AS created_at, NOW(6) AS updated_at")
    lines.append("FROM wc_products s")
    lines.append("JOIN brands b ON b.name = s.brand_name")
    lines.append("LEFT JOIN products p ON p.sku = s.sku")
    lines.append("ON DUPLICATE KEY UPDATE")
    lines.append("  name = VALUES(name),")
    lines.append("  price = VALUES(price),")
    lines.append("  brand_id = VALUES(brand_id),")
    lines.append("  category_id = VALUES(category_id),")
    lines.append("  image_url = VALUES(image_url),")
    lines.append("  description = VALUES(description),")
    lines.append("  is_active = VALUES(is_active),")
    lines.append("  is_featured = VALUES(is_featured),")
    lines.append("  is_deleted = VALUES(is_deleted),")
    lines.append("  stock_quantity = VALUES(stock_quantity),")
    lines.append("  seo_title = VALUES(seo_title),")
    lines.append("  seo_description = VALUES(seo_description),")
    lines.append("  tags = VALUES(tags),")
    lines.append("  updated_at = NOW(6);")
    lines.append("")
    lines.append("-- Upsert variants (unique by product_variants.sku)")
    lines.append("DROP TEMPORARY TABLE IF EXISTS wc_variants;")
    lines.append("CREATE TEMPORARY TABLE wc_variants (")
    lines.append("  sku VARCHAR(255) NOT NULL,")
    lines.append("  product_sku VARCHAR(50) NOT NULL,")
    lines.append("  variant_name VARCHAR(255) NOT NULL,")
    lines.append("  price DECIMAL(10,2) NOT NULL,")
    lines.append("  stock_quantity INT NOT NULL,")
    lines.append("  color VARCHAR(255) NULL,")
    lines.append("  size VARCHAR(255) NULL,")
    lines.append("  is_default BIT(1) NOT NULL")
    lines.append(") ENGINE=InnoDB;")
    for v in variants:
        lines.append(
            "INSERT INTO wc_variants (sku,product_sku,variant_name,price,stock_quantity,color,size,is_default) VALUES "
            f"({q(v.sku)},{q(v.product_sku)},{q(v.variant_name)},{v.price:.2f},{v.stock},{q(v.color)},{q(v.size)},{b(v.is_default)});"
        )
    lines.append("")
    lines.append("INSERT INTO product_variants (")
    lines.append("  is_active, is_default, price, stock_quantity, product_id, color, size, sku, variant_name, created_at, updated_at")
    lines.append(")")
    lines.append("SELECT")
    lines.append("  b'1' AS is_active, v.is_default, v.price, v.stock_quantity, p.id AS product_id, v.color, v.size, v.sku, v.variant_name, NOW(6), NOW(6)")
    lines.append("FROM wc_variants v")
    lines.append("JOIN products p ON p.sku = v.product_sku")
    lines.append("ON DUPLICATE KEY UPDATE")
    lines.append("  price = VALUES(price),")
    lines.append("  stock_quantity = VALUES(stock_quantity),")
    lines.append("  variant_name = VALUES(variant_name),")
    lines.append("  color = VALUES(color),")
    lines.append("  size = VALUES(size),")
    lines.append("  is_default = VALUES(is_default),")
    lines.append("  updated_at = NOW(6);")
    lines.append("")
    lines.append("-- Insert images (4 per product, refreshed)")
    lines.append("DROP TEMPORARY TABLE IF EXISTS wc_images;")
    lines.append("CREATE TEMPORARY TABLE wc_images (")
    lines.append("  product_sku VARCHAR(50) NOT NULL,")
    lines.append("  image_url VARCHAR(500) NOT NULL,")
    lines.append("  alt_text VARCHAR(200) NULL,")
    lines.append("  is_primary BIT(1) NOT NULL,")
    lines.append("  display_order INT NOT NULL")
    lines.append(") ENGINE=InnoDB;")
    for im in images:
        lines.append(
            "INSERT INTO wc_images (product_sku,image_url,alt_text,is_primary,display_order) VALUES "
            f"({q(im.product_sku)},{q(im.url)},{q(im.alt)},{b(im.is_primary)},{im.order});"
        )
    lines.append("")
    lines.append("INSERT INTO product_images (product_id, image_url, alt_text, is_primary, display_order, created_at, updated_at)")
    lines.append("SELECT p.id, i.image_url, i.alt_text, i.is_primary, i.display_order, NOW(6), NOW(6)")
    lines.append("FROM wc_images i")
    lines.append("JOIN products p ON p.sku = i.product_sku;")
    lines.append("")
    lines.append("-- Recompute product stock from variants (inventory correctness)")
    lines.append("UPDATE products p")
    lines.append("JOIN (SELECT product_id, SUM(stock_quantity) AS sum_stock FROM product_variants GROUP BY product_id) s")
    lines.append("  ON p.id = s.product_id")
    lines.append("SET p.stock_quantity = s.sum_stock, p.updated_at = NOW(6)")
    lines.append("WHERE p.sku LIKE 'WC-%';")
    lines.append("")
    lines.append("-- Verify")
    lines.append("SELECT 'WC products' AS label, COUNT(*) AS cnt FROM products WHERE sku LIKE 'WC-%' AND IFNULL(is_deleted,0)=0;")
    lines.append("SELECT 'WC variants' AS label, COUNT(*) AS cnt FROM product_variants WHERE sku LIKE 'WC-%';")
    lines.append("SELECT 'WC images' AS label, COUNT(*) AS cnt FROM product_images WHERE product_id COLLATE utf8mb4_unicode_ci IN (SELECT id COLLATE utf8mb4_unicode_ci FROM products WHERE sku LIKE 'WC-%');")
    lines.append("SET SQL_SAFE_UPDATES = 1;")
    lines.append("")
    return "\n".join(lines)


def main() -> None:
    products, variants, images = build_catalog()
    sql = generate_sql(products, variants, images)
    out_path = "Buildd30_7/Buildd43/sql/seed_world_class_catalog.sql"
    with open(out_path, "w", encoding="utf-8") as f:
        f.write(sql)
    print(f"Wrote {out_path} ({len(products)} products, {len(variants)} variants, {len(images)} images)")


if __name__ == "__main__":
    main()


