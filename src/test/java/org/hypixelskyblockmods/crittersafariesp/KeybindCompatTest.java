package org.hypixelskyblockmods.crittersafariesp;

import com.mojang.blaze3d.platform.InputConstants;
import org.hypixelskyblockmods.crittersafariesp.platform.ClientCompat;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class KeybindCompatTest {
    @Test void letterBindingsRemainKeyboardKeys() {
        for (String letter : new String[]{"a", "b", "c", "d", "e", "f"}) {
            var key = InputConstants.getKey("key.keyboard." + letter);
            assertEquals(key, ClientCompat.configKey(ClientCompat.configCode(key)));
            assertEquals(ClientCompat.keyboardType(), ClientCompat.configKey(ClientCompat.configCode(key)).getType());
        }
    }
    @Test void mouseAndUnboundBindingsRoundTrip() {
        for (String name : new String[]{"key.mouse.left", "key.mouse.right", "key.mouse.middle"}) {
            var key = InputConstants.getKey(name);
            assertEquals(key, ClientCompat.configKey(ClientCompat.configCode(key)));
        }
        assertEquals(InputConstants.UNKNOWN, ClientCompat.configKey(-1));
        assertEquals(-1, ClientCompat.configCode(InputConstants.UNKNOWN));
    }
}
