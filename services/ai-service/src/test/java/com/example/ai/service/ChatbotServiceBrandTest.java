package com.example.ai.service;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ChatbotServiceBrandTest {

    @Test
    void onePlusQuery_shouldNotProduceNoisyOneOrPlusKeywords() throws Exception {
        ChatbotService svc = new ChatbotService();

        Method m = ChatbotService.class.getDeclaredMethod("extractKeywords", String.class);
        m.setAccessible(true);

        String q = "có sản phẩm nào của one plus không";
        String[] kws = (String[]) m.invoke(svc, q);

        List<String> list = java.util.Arrays.asList(kws);
        assertTrue(list.contains("oneplus"), "Expected 'oneplus' keyword");
        assertFalse(list.contains("one"), "Should not include noisy keyword 'one'");
        assertFalse(list.contains("plus"), "Should not include noisy keyword 'plus'");
    }

    @Test
    void onePlusBrandFilter_shouldBeDetectedWhenUserSaysCua() throws Exception {
        ChatbotService svc = new ChatbotService();

        Method m = ChatbotService.class.getDeclaredMethod("extractBrandFilter", String.class);
        m.setAccessible(true);

        assertEquals("oneplus", m.invoke(svc, "có sản phẩm của one plus không"));
        assertEquals("oneplus", m.invoke(svc, "có sản phẩm của oneplus không"));
    }

    @Test
    void softBrandPreference_shouldBeDetectedWithoutCua() throws Exception {
        ChatbotService svc = new ChatbotService();

        Method m = ChatbotService.class.getDeclaredMethod("extractSoftBrandPreference", String.class);
        m.setAccessible(true);

        assertEquals("oneplus", m.invoke(svc, "one plus 13 5g có không"));
    }

    @Test
    void iphoneQuery_shouldDetectAppleBrandAndPhoneCategory_andIphoneProductLine() throws Exception {
        ChatbotService svc = new ChatbotService();

        Method softBrand = ChatbotService.class.getDeclaredMethod("extractSoftBrandPreference", String.class);
        softBrand.setAccessible(true);
        assertEquals("apple", softBrand.invoke(svc, "có điện thoại iphone nào không"));

        Method cat = ChatbotService.class.getDeclaredMethod("extractCategoryPreference", String.class);
        cat.setAccessible(true);
        assertEquals("phone", cat.invoke(svc, "có điện thoại iphone nào không"));

        Method line = ChatbotService.class.getDeclaredMethod("extractProductLinePreference", String.class);
        line.setAccessible(true);
        assertEquals("iphone", line.invoke(svc, "có điện thoại iphone nào không"));
    }

    @Test
    void formatPrice_shouldAddSeparators_andUpscaleForHighTicketCategories() throws Exception {
        ChatbotService svc = new ChatbotService();

        Method m = ChatbotService.class.getDeclaredMethod("formatPrice", Object.class, String.class);
        m.setAccessible(true);

        // Also verify the 1-arg wrapper exists (keeps backward compatibility and avoids dead-code warnings)
        Method wrapper = ChatbotService.class.getDeclaredMethod("formatPrice", Object.class);
        wrapper.setAccessible(true);
        assertNotNull(wrapper.invoke(svc, 1000));

        // low ticket: do not upscale
        assertEquals("28.990", m.invoke(svc, 28990, "Accessories"));

        // high ticket: upscale thousand-VND -> VND
        assertEquals("19.990.000", m.invoke(svc, 19990, "Laptops"));
        assertEquals("12.650.000", m.invoke(svc, 12650, "Phones"));
    }
}

