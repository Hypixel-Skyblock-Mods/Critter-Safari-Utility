package org.hypixelskyblockmods.crittersafariesp;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import org.hypixelskyblockmods.crittersafariesp.platform.ClientCompat;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.fabricmc.loader.api.FabricLoader;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import io.github.notenoughupdates.moulconfig.gui.editors.GuiOptionEditorText;
import io.github.notenoughupdates.moulconfig.gui.component.TextFieldComponent;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.Items;
import com.mojang.math.Transformation;
import org.joml.Vector3f;
import org.joml.Quaternionf;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.ambient.Bat;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

@SuppressWarnings({"UnstableApiUsage", "unchecked"})
public final class CritterClientGameTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        // No dedicated server or network session: the test world is temporary and local.
        Settings original = CritterSafariClient.settings;
        try (var singleplayer = context.worldBuilder().create()) {
            try {
            singleplayer.getClientLevel().waitForChunksRender();
            context.runOnClient(mc -> {
                var settings = new Settings(); settings.safariOnly = false;
                CritterSafariClient.apply(settings);
                var p = mc.player.position();
                var beeType = (EntityType<? extends Bee>) BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("bee"));
                Bee bee = new Bee(beeType, mc.level);
                bee.setId(-10001); bee.setPos(p.x + 4, p.y + 0.25, p.z + 8);
                bee.setCustomName(Component.literal("Honeybug"));
                mc.level.addEntity(bee);
                Bee far = new Bee(beeType, mc.level);
                far.setId(-10002); far.setPos(p.x, p.y, p.z + 201);
                far.setCustomName(Component.literal("Out of range Honeybug"));
                mc.level.addEntity(far);
                mc.player.setYRot(0); mc.player.setXRot(0);
            });
            context.waitFor(mc -> CritterSafariClient.tracker.targets().stream().anyMatch(t -> t.entity().getId() == -10001));
            context.runOnClient(mc -> {
                var near = mc.level.getEntity(-10001);
                if (near.isCurrentlyGlowing()) throw new AssertionError("ESP added entity silhouette glow");
                if (CritterSafariClient.tracker.targets().stream().anyMatch(t -> t.entity().getId() == -10002))
                    throw new AssertionError("Out-of-range entity was selected");
            });
            verifyRadiusAndProps(context);
            context.waitTicks(10);
            context.takeScreenshot("critter-tracer-fill");
            context.setScreen(() -> new ChatScreen("",false));
            context.waitTicks(2);
            context.runOnClient(mc -> {
                if(!TracerRenderer.screenAllowsEsp(ClientCompat.screen(mc)) || !selected(-10001))
                    throw new AssertionError("Opening chat hid or cleared the ESP");
            });
            context.takeScreenshot("critter-chat-visible");
            context.runOnClient(mc -> ClientCompat.setScreen(mc,null));
            verifyRunPanels(context);
            verifyFloorDropsAndModelBounds(context);
            verifyGroupedModelsAndCrowd(context);
            context.runOnClient(mc -> {
                var stack = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace("snowball")));
                var tag = new CompoundTag(); var attributes = new CompoundTag();
                attributes.putString("id", "CRITTER_CAPSULE"); tag.put("ExtraAttributes", attributes);
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                mc.player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                mc.player.setYRot(-26.565f); mc.player.setXRot(4);
            });
            context.waitTicks(5);
            context.runOnClient(mc -> {
                var path = CapsulePreview.predict(mc, CritterSafariClient.settings, 1);
                if (path == null || !Integer.valueOf(-10001).equals(path.hitEntity()))
                    throw new AssertionError("Capsule preview did not predict the aimed critter hit");
            });
            context.takeScreenshot("critter-predicted-hit");
            context.runOnClient(mc -> {
                mc.level.getEntity(-10001).setDeltaMovement(Vec3.ZERO);
                mc.player.setYRot(0); mc.player.setXRot(0);
            });
            context.waitTicks(1);
            context.runOnClient(mc -> {
                if(CapsulePreview.lead(mc,CritterSafariClient.settings,1)==null)
                    throw new AssertionError("Stationary critter in the general viewing direction had no aim circle");
                if(!Integer.valueOf(-10001).equals(CapsulePreview.preview(mc,CritterSafariClient.settings,1).aimEntity()))
                    throw new AssertionError("Aim circle didn't select the reachable critter");
                CritterSafariClient.settings.predictMotion=false;
                if(CapsulePreview.lead(mc,CritterSafariClient.settings,1)==null)
                    throw new AssertionError("Disabling motion prediction hid the stationary aim circle");
                CritterSafariClient.settings.predictMotion=true;
                mc.player.setYRot(180);
                if(CapsulePreview.lead(mc,CritterSafariClient.settings,1)!=null)
                    throw new AssertionError("Aim circle appeared for a critter behind the player");
                mc.player.setYRot(0);
            });
            context.takeScreenshot("critter-stationary-aim-circle");
            context.runOnClient(mc -> { mc.player.setYRot(-26.565f); mc.player.setXRot(4); });
            context.runOnClient(mc -> mc.level.getEntity(-10001).setDeltaMovement(new Vec3(.15,0,0)));
            context.waitTicks(1);
            context.runOnClient(mc -> {
                var lead=CapsulePreview.lead(mc,CritterSafariClient.settings,1);
                if(lead==null || lead.direction().x<=0) throw new AssertionError("Moving critter had no lead aim circle");
            });
            context.takeScreenshot("critter-moving-aim-circle");
            context.runOnClient(mc -> mc.level.getEntity(-10001).setDeltaMovement(Vec3.ZERO));
            context.runOnClient(mc -> { mc.player.setYRot(0); mc.player.setXRot(0); });
            context.waitTicks(5);
            context.runOnClient(mc -> {
                var path = CapsulePreview.predict(mc, CritterSafariClient.settings, 1);
                if (path == null || path.hitEntity() != null)
                    throw new AssertionError("A missed throw predicted an entity hit");
            });
            context.takeScreenshot("critter-predicted-miss");
            verifyLookedAtNamesAndReachability(context);
            verifyUnnamedCaughtAndAmbientFish(context,singleplayer);
            verifySpecialNpcAndMounds(context,singleplayer);
            context.runOnClient(mc -> mc.player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY));
            context.setScreen(() -> new SettingsScreen(null));
            context.waitTicks(20);
            context.runOnClient(mc -> {
                var screen = (SettingsScreen)ClientCompat.screen(mc);
                if (screen.editor.getAllCategories().size() != 4)
                    throw new AssertionError("Expected all four MoulConfig categories");
            });
            context.takeScreenshot("critter-settings-general");
            context.runOnClient(mc -> {
                var screen = (SettingsScreen)ClientCompat.screen(mc);
                screen.editor.setSelectedCategory(screen.editor.getAllCategories().values().stream()
                    .filter(c -> c.getIdentifier().endsWith("appearance")).findFirst().orElseThrow());
            });
            context.waitTicks(10);
            context.takeScreenshot("critter-settings-appearance");
            context.runOnClient(mc -> {
                var screen = (SettingsScreen)ClientCompat.screen(mc);
                screen.editor.setSelectedCategory(screen.editor.getAllCategories().values().stream()
                    .filter(c -> c.getIdentifier().endsWith("detection")).findFirst().orElseThrow());
                screen.config.general.radius = "85";
            });
            context.waitTicks(10);
            context.takeScreenshot("critter-settings-detection");
            context.runOnClient(mc -> {
                var screen = (SettingsScreen)ClientCompat.screen(mc);
                screen.editor.setSelectedCategory(screen.editor.getAllCategories().values().stream()
                    .filter(c -> c.getIdentifier().endsWith("trajectory")).findFirst().orElseThrow());
                screen.config.trajectory.speed = 1.7f;
            });
            context.waitTicks(10);
            context.takeScreenshot("critter-settings-trajectory");
            context.runOnClient(mc -> ((SettingsScreen)ClientCompat.screen(mc)).config.general.moveHud.run());
            context.runOnClient(mc -> {
                var screen = (HudPositionScreen)ClientCompat.screen(mc);
                var left = new MouseButtonInfo(0, 0);
                if (!screen.mouseClicked(new MouseButtonEvent(10, 10, left), false))
                    throw new AssertionError("HUD badge could not be grabbed");
                screen.mouseDragged(new MouseButtonEvent(110, 90, left), 100, 80);
                screen.mouseReleased(new MouseButtonEvent(110, 90, left));
                if(!screen.mouseScrolled(110,90,0,1) || CritterSafariClient.settings.hudScale<=1)
                    throw new AssertionError("Status badge did not resize on scroll");
                var forest=BiomePanels.rect(CritterSafariClient.settings,SafariSpecies.Biome.FOREST,screen.width,screen.height);
                if(!screen.mouseClicked(new MouseButtonEvent(forest.x()+8,forest.y()+5,left),false))
                    throw new AssertionError("Unified HUD editor could not grab a biome panel");
                screen.mouseDragged(new MouseButtonEvent(forest.x()+28,forest.y()+35,left),20,30);
                screen.mouseReleased(new MouseButtonEvent(forest.x()+28,forest.y()+35,left));
                var moved=BiomePanels.rect(CritterSafariClient.settings,SafariSpecies.Biome.FOREST,screen.width,screen.height);
                if(!screen.mouseScrolled(moved.x()+8,moved.y()+5,0,-1) || CritterSafariClient.settings.biomeScale[1]>=1)
                    throw new AssertionError("Biome panel did not resize independently on scroll");
            });
            context.waitTicks(10);
            context.takeScreenshot("critter-hud-placement");
            context.runOnClient(mc -> ClientCompat.screen(mc).onClose());
            context.waitTicks(20);
            context.runOnClient(mc -> ClientCompat.screen(mc).onClose());
            context.runOnClient(mc -> {
                if (ClientCompat.screen(mc) != null) throw new AssertionError("MoulConfig did not close");
                if (CritterSafariClient.settings.radius != 85) throw new AssertionError("MoulConfig changes were not applied");
                if (CritterSafariClient.settings.ballSpeed != 1.7f) throw new AssertionError("Trajectory settings were not saved");
                if (CritterSafariClient.settings.hudX <= 0 || CritterSafariClient.settings.hudY <= 0)
                    throw new AssertionError("MoulConfig overwrote the dragged HUD position");
                if(CritterSafariClient.settings.biomeY[1]<=Settings.defaultBiomeY()[1])
                    throw new AssertionError("MoulConfig overwrote the dragged biome panel position");
                if(CritterSafariClient.settings.hudScale<=1 || CritterSafariClient.settings.biomeScale[1]>=1)
                    throw new AssertionError("MoulConfig overwrote the HUD sizes");
                mc.level.removeEntity(-10001, Entity.RemovalReason.DISCARDED);
                mc.level.removeEntity(-10002, Entity.RemovalReason.DISCARDED);
            });
            context.waitTicks(1);
            context.runOnClient(mc -> {
                if (CritterSafariClient.tracker.targets().stream().anyMatch(t -> t.entity().getId() == -10001))
                    throw new AssertionError("Removed entity remained tracked");
                if (!CritterSafariClient.tracker.targets().isEmpty()) throw new AssertionError("Empty scene kept targets");
            });
            context.takeScreenshot("critter-empty-hud");
            context.runOnClient(mc -> CritterSafariClient.settings.enabled = false);
            context.waitTicks(1);
            context.runOnClient(mc -> {
                if (!CritterSafariClient.tracker.targets().isEmpty()) throw new AssertionError("Disabled ESP kept targets");
            });
            } catch(RuntimeException | AssertionError failure) {
                context.runOnClient(mc -> { CritterSafariClient.LOGGER.error("Local ESP test failed",failure); ClientCompat.setScreen(mc,null); });
                context.waitTicks(1);
                throw failure;
            }
        } finally {
            context.runOnClient(mc -> CritterSafariClient.apply(original));
        }
    }
    private static void verifyRunPanels(ClientGameTestContext context) {
        String canceled="CAPTURE! You caught a Sparkling Honeybug and gained a Honeybug Shard!";
        ClientReceiveMessageEvents.ALLOW_GAME.register((message,overlay) -> !message.getString().equals(canceled));
        context.runOnClient(mc -> {
            CritterSafariClient.settings.oneCritterMode=true;
            CritterSafariClient.apply(CritterSafariClient.settings);
            if(!ClientReceiveMessageEvents.ALLOW_GAME.invoker().allowReceiveGameMessage(Component.literal(
                "CAPTURE! You caught a Honeybug and gained a Honeybug Shard!"),false))
                throw new AssertionError("Capture listener canceled the server message");
            if(selected(-10001)) throw new AssertionError("One Critter Mode left a captured species visible");
        });
        context.setScreen(() -> new ChatScreen("",false)); context.waitTicks(2);
        context.takeScreenshot("critter-one-mode-caught");
        context.runOnClient(mc -> {
            var screen=ClientCompat.screen(mc); var box=BiomePanels.rect(CritterSafariClient.settings,SafariSpecies.Biome.FOREST,screen.width,screen.height);
            var left=new MouseButtonInfo(0,0);
            float scale=BiomePanels.scale(CritterSafariClient.settings,SafariSpecies.Biome.FOREST,screen.width,screen.height);
            int index=SafariSpecies.Biome.FOREST.species().indexOf(SafariSpecies.HONEYBUG);
            if(ScreenMouseEvents.allowMouseClick(screen).invoker().allowMouseClick(screen,
                new MouseButtonEvent(box.x()+(10+(index%3)*BiomePanels.CELL)*scale,box.y()+(BiomePanels.HEADER+(index/3)*BiomePanels.ROW+4)*scale,left)))
                throw new AssertionError("Chat species click wasn't consumed");
            if(!selected(-10001)) throw new AssertionError("Manual re-enable didn't restore captured species immediately");
            ScreenMouseEvents.allowMouseClick(screen).invoker().allowMouseClick(screen,
                new MouseButtonEvent(box.x()+box.width()-15,box.y()+box.height()-5,left));
            if(selected(-10001)) throw new AssertionError("Disable-all didn't hide the biome");
            ScreenMouseEvents.allowMouseClick(screen).invoker().allowMouseClick(screen,
                new MouseButtonEvent(box.x()+25,box.y()+box.height()-7,left));
            if(!selected(-10001)) throw new AssertionError("Enable-all didn't restore the biome");
            ClientReceiveMessageEvents.ALLOW_GAME.invoker().allowReceiveGameMessage(Component.literal(
                "LOOT SHARE! You received 2x Honeybug Shard from Another catching a Honeybug!"),false);
            if(selected(-10001) || !CritterSafariClient.run.shared(SafariSpecies.HONEYBUG))
                throw new AssertionError("Loot-share catch didn't disable the species");
            ClientReceiveMessageEvents.ALLOW_GAME.invoker().allowReceiveGameMessage(Component.literal(
                "[MVP+] Other entered Critter Safari!"),false);
            if(selected(-10001)) throw new AssertionError("Another player's entry reset the checklist");
        });
        context.takeScreenshot("critter-one-mode-loot-shared");
        context.runOnClient(mc -> {
            ClientReceiveMessageEvents.ALLOW_GAME.invoker().allowReceiveGameMessage(Component.literal(
                "[MVP+] "+mc.player.getGameProfile().name()+" entered Critter Safari!"),false);
            if(!selected(-10001)) throw new AssertionError("New Safari didn't re-enable species");
            for(var species:SafariSpecies.values()) if(!CritterSafariClient.run.enabled(species) || CritterSafariClient.run.caught(species))
                throw new AssertionError("New Safari left a species disabled/caught");
            var screen=ClientCompat.screen(mc); var box=BiomePanels.rect(CritterSafariClient.settings,SafariSpecies.Biome.ICY,screen.width,screen.height);
            var left=new MouseButtonInfo(0,0);
            ScreenMouseEvents.allowMouseClick(screen).invoker().allowMouseClick(screen,new MouseButtonEvent(box.x()+8,box.y()+5,left));
            ScreenMouseEvents.allowMouseDrag(screen).invoker().allowMouseDrag(screen,new MouseButtonEvent(box.x()-20,box.y()+30,left),-28,25);
            ScreenMouseEvents.allowMouseRelease(screen).invoker().allowMouseRelease(screen,new MouseButtonEvent(box.x()-20,box.y()+30,left));
            if(CritterSafariClient.settings.biomeY[3]<=Settings.defaultBiomeY()[3]) throw new AssertionError("Chat header drag didn't move the panel");
        });
        context.takeScreenshot("critter-biome-panel-drag");
        context.setScreen(() -> new InventoryScreen(net.minecraft.client.Minecraft.getInstance().player));
        context.waitTicks(1);
        context.runOnClient(mc -> {
            var screen=ClientCompat.screen(mc);
            var box=BiomePanels.rect(CritterSafariClient.settings,SafariSpecies.Biome.FOREST,screen.width,screen.height);
            float scale=BiomePanels.scale(CritterSafariClient.settings,SafariSpecies.Biome.FOREST,screen.width,screen.height);
            int index=SafariSpecies.Biome.FOREST.species().indexOf(SafariSpecies.HONEYBUG);
            if(!BiomePanels.interactive(screen) || ScreenMouseEvents.allowMouseClick(screen).invoker().allowMouseClick(screen,
                new MouseButtonEvent(box.x()+(10+(index%3)*BiomePanels.CELL)*scale,box.y()+(BiomePanels.HEADER+(index/3)*BiomePanels.ROW+4)*scale,new MouseButtonInfo(0,0)))
                || selected(-10001)) throw new AssertionError("A non-chat cursor screen could not toggle a species");
        });
        context.takeScreenshot("critter-inventory-panel-toggle");
        context.runOnClient(mc -> {
            CritterSafariClient.run.setAll(SafariSpecies.Biome.FOREST,true);
            if(ClientReceiveMessageEvents.ALLOW_GAME.invoker().allowReceiveGameMessage(Component.literal(canceled),false)
                || selected(-10001)) throw new AssertionError("A chat-hiding mod prevented capture tracking");
            ClientCompat.setScreen(mc,null);
            CritterSafariClient.settings.oneCritterMode=false;
            CritterSafariClient.settings.biomeX=Settings.defaultBiomeX(); CritterSafariClient.settings.biomeY=Settings.defaultBiomeY();
            CritterSafariClient.run.reset(); CritterSafariClient.apply(CritterSafariClient.settings);
        });
    }
    private static void verifyRadiusAndProps(ClientGameTestContext context) {
        context.runOnClient(mc -> {
            var p = mc.player.position();
            var displayType = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("item_display"));
            var prop = new Display.ItemDisplay(displayType, mc.level);
            prop.setId(-20001); prop.setPos(p.x + 3, p.y + 40, p.z + 2); mc.level.addEntity(prop);
            var npc = new RemotePlayer(mc.level, new GameProfile(UUID.randomUUID(), "SafariManager"));
            npc.setId(-20002); npc.setPos(p.x + 3, p.y, p.z); npc.setCustomName(Component.literal("Safari Manager")); mc.level.addEntity(npc);
            var interaction = new Interaction(BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("interaction")), mc.level);
            interaction.setId(-20003); interaction.setPos(p.x + 4, p.y, p.z); mc.level.addEntity(interaction);
            var named = new Display.ItemDisplay(displayType, mc.level);
            named.getSlot(0).set(new ItemStack(Items.STONE));
            named.setId(-20004); named.setPos(p.x + 20, p.y, p.z); named.setCustomName(Component.literal("Litterbug")); mc.level.addEntity(named);
            var distant = new RemotePlayer(mc.level, new GameProfile(UUID.randomUUID(), "Snoozle"));
            distant.setId(-20005); distant.setPos(p.x + 80, p.y, p.z); distant.setCustomName(Component.literal("Snoozle")); mc.level.addEntity(distant);
            var body = new Interaction(BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("interaction")), mc.level);
            body.setId(-20006); body.setPos(p.x - 6, p.y, p.z); mc.level.addEntity(body);
            var label = new ArmorStand((EntityType<? extends ArmorStand>)BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("armor_stand")), mc.level);
            label.setId(-20007); label.setPos(p.x - 6, p.y + 1.5, p.z);
            label.setCustomName(Component.literal("Sparkling Honeybug 20❤")); mc.level.addEntity(label);
            var orphan = new ArmorStand((EntityType<? extends ArmorStand>)BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("armor_stand")), mc.level);
            orphan.setId(-20008); orphan.setPos(p.x+40,p.y+30,p.z); orphan.setCustomName(Component.literal("Honeybug")); mc.level.addEntity(orphan);
            var trade = new ArmorStand((EntityType<? extends ArmorStand>)BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("armor_stand")), mc.level);
            trade.setId(-20009); trade.setPos(p.x+3,p.y+1.5,p.z); trade.setCustomName(Component.literal("Snoozle Shard")); mc.level.addEntity(trade);
            CritterSafariClient.settings.radius = 100;
            CritterSafariClient.apply(CritterSafariClient.settings);
        });
        context.waitTicks(2);
        context.runOnClient(mc -> {
            if (!selected(-20004) || !selected(-20005) || !selected(-20006) || selected(-20007))
                throw new AssertionError("Named generic critters/nameplate association were not detected correctly at 100 blocks");
            if (selected(-20001) || selected(-20002) || selected(-20003) || selected(-20008) || selected(-20009))
                throw new AssertionError("Default detection selected flying props, orphan names or trade NPCs");
            CritterSafariClient.settings.detection = "BOTH"; CritterSafariClient.tracker.refresh();
        });
        context.waitTicks(1);
        context.runOnClient(mc -> {
            if (selected(-20001) || selected(-20002) || selected(-20003)) throw new AssertionError("Mixed mode selected generic props or NPCs");
            CritterSafariClient.settings.detection = "NAME"; CritterSafariClient.tracker.refresh();
        });
        context.setScreen(() -> new SettingsScreen(null));
        context.waitTicks(20);
        context.runOnClient(mc -> {
            var screen = (SettingsScreen)ClientCompat.screen(mc);
            java.lang.reflect.Field radiusField;
            try { radiusField = SettingsScreen.General.class.getField("radius"); }
            catch (NoSuchFieldException e) { throw new AssertionError(e); }
            var option = screen.editor.getOptionFromField(radiusField);
            screen.editor.goToOption(option);
            var slider = (GuiOptionEditorText)option.getEditor();
            TextFieldComponent input = slider.getDelegate().foldRecursive((TextFieldComponent)null,
                (component, found) -> component instanceof TextFieldComponent field ? field : found);
            if (input == null) throw new AssertionError("Native radius numeric input not found");
            input.getText().get();
            input.requestFocus();
        });
        context.getInput().holdControl(); context.getInput().pressKey(GLFW.GLFW_KEY_A); context.getInput().releaseControl();
        context.getInput().typeChars("12");
        context.waitTicks(1);
        context.takeScreenshot("radius-exact-twelve");
        context.runOnClient(mc -> {
            var screen = (SettingsScreen)ClientCompat.screen(mc);
            if (!screen.config.general.radius.equals("12")) throw new AssertionError("Native numeric radius edit produced " + screen.config.general.radius);
            if (CritterSafariClient.settings.radius != 12) throw new AssertionError("Typed radius was not applied while editing");
            if (!selected(-10001) || !selected(-20006) || selected(-20004) || selected(-20005) || selected(-10002))
                throw new AssertionError("Live 10-block radius retained a distant target or lost the nearby critter");
            try {
                var saved = new ConfigStore(FabricLoader.getInstance().getConfigDir().resolve("crittersafariesp.json")).load();
                if (saved.radius != 12) throw new AssertionError("Native radius edit was not persisted to disk");
            } catch (java.io.IOException e) { throw new AssertionError("Could not reload saved radius", e); }
        });
        context.getInput().pressKey(GLFW.GLFW_KEY_ESCAPE);
        if (context.computeOnClient(mc -> ClientCompat.screen(mc) instanceof SettingsScreen))
            context.getInput().pressKey(GLFW.GLFW_KEY_ESCAPE);
        context.runOnClient(mc -> {
            if (ClientCompat.screen(mc) instanceof SettingsScreen) throw new AssertionError("Escape did not close radius settings");
            ClientCompat.setScreen(mc, null); // A subsequent Escape can open Minecraft's pause menu.
        });
        context.waitTicks(1);
        context.runOnClient(mc -> {
            if (CritterSafariClient.settings.radius != 12) throw new AssertionError("Typed radius was not saved");
            if (!selected(-10001) || selected(-20004) || selected(-20005)) throw new AssertionError("10-block radius retained a 12/200-block target or lost the nearby critter");
        });
        context.setScreen(() -> new SettingsScreen(null));
        context.waitTicks(20);
        context.runOnClient(mc -> {
            var screen = (SettingsScreen)ClientCompat.screen(mc);
            if (!screen.config.general.radius.equals("12")) throw new AssertionError("Reopened radius editor lost the applied value");
            var option=screen.editor.getOptionFromField(radiusField()); screen.editor.goToOption(option);
            ((GuiOptionEditorText)option.getEditor()).getDelegate().foldRecursive((TextFieldComponent)null,
                (component,found)->component instanceof TextFieldComponent field?field:found).requestFocus();
        });
        context.getInput().holdControl(); context.getInput().pressKey(GLFW.GLFW_KEY_A); context.getInput().releaseControl();
        context.getInput().typeChars("100"); context.waitTicks(1);
        context.runOnClient(mc -> {
            if(CritterSafariClient.settings.radius!=100 || !selected(-20004) || !selected(-20005))
                throw new AssertionError("Increasing radius from 12 to 100 through the native input retained a hidden range cap");
            ClientCompat.setScreen(mc, null);
            CritterSafariClient.settings.radius = 5; CritterSafariClient.tracker.refresh();
        });
        context.waitTicks(1);
        context.runOnClient(mc -> {
            if (selected(-10001) || selected(-20006)) throw new AssertionError("Shrinking radius did not remove nearby targets immediately");
            CritterSafariClient.settings.radius = 10; CritterSafariClient.settings.detection = "TYPE";
            CritterSafariClient.tracker.refresh();
        });
        context.waitTicks(1);
        context.runOnClient(mc -> {
            if (!selected(-20002) || !selected(-20003) || selected(-20004) || selected(-20005) || selected(-20001))
                throw new AssertionError("Raw type matching bypassed the 10-block spherical radius");
            for (int id : new int[]{-20001,-20002,-20003,-20004,-20005,-20006,-20007,-20008,-20009}) mc.level.removeEntity(id, Entity.RemovalReason.DISCARDED);
            CritterSafariClient.settings.detection = "NAME";
            CritterSafariClient.settings.radius = 100; CritterSafariClient.apply(CritterSafariClient.settings);
        });
        context.waitTicks(1);
        context.runOnClient(mc -> { if (!selected(-10001)) throw new AssertionError("Expanding radius did not restore the target"); });
    }
    private static boolean selected(int id) { return CritterSafariClient.tracker.targets().stream().anyMatch(t -> t.entity().getId() == id); }
    private static void verifyLookedAtNamesAndReachability(ClientGameTestContext context) {
        var savedNames=new java.util.ArrayList<>(CritterSafariClient.settings.names);
        context.runOnClient(mc -> {
            CritterSafariClient.settings.names=new java.util.ArrayList<>(java.util.List.of("Aim Test"));
            CritterSafariClient.settings.radius=100;
            var bee=mc.level.getEntity(-10001); bee.setCustomName(Component.literal("Aim Test"));
            var p=mc.player.position(); bee.setPos(p.x+10,p.y+.75,p.z+10);
            mc.player.setYRot(0); mc.player.setXRot(0); CritterSafariClient.tracker.refresh();
            if(!Integer.valueOf(-10001).equals(CapsulePreview.preview(mc,CritterSafariClient.settings,1).aimEntity()))
                throw new AssertionError("Reachable critter 45 degrees away was blocked by a viewing-angle cutoff");
            bee.setPos(p.x,p.y+.75,p.z+50);
            mc.player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            CritterSafariClient.tracker.refresh();
        });
        context.waitTicks(2);
        context.runOnClient(mc -> {
            if(!TracerRenderer.lastLabelNames.contains("Aim Test"))
                throw new AssertionError("Directly viewed 50-block critter had no nametag without a capsule");
        });
        context.takeScreenshot("distant-looked-at-name");
        context.runOnClient(mc -> {
            var p=mc.player.position(); mc.level.getEntity(-10001).setPos(p.x,p.y+.75,p.z+8);
            var base=BlockPos.containing(p);
            for(int x=-2;x<=2;x++) for(int y=-1;y<=32;y++)
                mc.level.setBlock(base.offset(x,y,4),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
            CritterSafariClient.tracker.refresh();
        });
        context.waitTicks(2);
        context.runOnClient(mc -> {
            if(!TracerRenderer.lastLabelNames.contains("Aim Test"))
                throw new AssertionError("Wall hid the independently rendered ESP nametag");
            var stack=new ItemStack(Items.SNOWBALL);
            var tag=new CompoundTag(); var attributes=new CompoundTag();
            attributes.putString("id","CRITTER_CAPSULE"); tag.put("ExtraAttributes",attributes);
            stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
            mc.player.setItemInHand(InteractionHand.MAIN_HAND,stack);
            if(CapsulePreview.lead(mc,CritterSafariClient.settings,1)!=null)
                throw new AssertionError("An obstructed critter incorrectly had a reachable aim circle");
        });
        context.waitTicks(2);
        context.takeScreenshot("name-through-wall-no-hit-circle");
        context.runOnClient(mc -> {
            var p=mc.player.position(); var base=BlockPos.containing(p);
            for(int x=-2;x<=2;x++) for(int y=-1;y<=32;y++)
                mc.level.setBlock(base.offset(x,y,4),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
            CritterSafariClient.settings.names=savedNames;
            var bee=mc.level.getEntity(-10001); bee.setCustomName(Component.literal("Honeybug"));
            bee.setPos(p.x+4,p.y+.25,p.z+8); CritterSafariClient.tracker.refresh();
        });
    }
    private static void verifyUnnamedCaughtAndAmbientFish(ClientGameTestContext context,net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext singleplayer) {
        // The biome inference fixture uses actual Safari map coordinates in this local world.
        singleplayer.getServer().runCommand("tp @a 1 70 20");
        context.waitFor(mc -> mc.player.position().distanceToSqr(new Vec3(1,70,20))<4);
        singleplayer.getClientLevel().waitForChunksRender();
        context.runOnClient(mc -> {
            CritterSafariClient.settings.detection="BOTH"; CritterSafariClient.settings.radius=100;
            CritterSafariClient.settings.oneCritterMode=false;
            var p=mc.player.position();
            var fishType=(EntityType<? extends net.minecraft.world.entity.animal.fish.TropicalFish>)
                BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("tropical_fish"));
            var ambient=new net.minecraft.world.entity.animal.fish.TropicalFish(fishType,mc.level);
            ambient.setId(-60001); ambient.setPos(p.x+12,p.y,p.z+8); mc.level.addEntity(ambient);
            var named=new net.minecraft.world.entity.animal.fish.TropicalFish(fishType,mc.level);
            named.setId(-60002); named.setPos(p.x+8,p.y,p.z+3);
            named.setCustomName(Component.literal("Cavernfish")); mc.level.addEntity(named);
            var beeType=(EntityType<? extends Bee>)BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("bee"));
            var namedBee=new Bee(beeType,mc.level); namedBee.setId(-60003);
            namedBee.setPos(1,66,27); namedBee.setCustomName(Component.literal("Honeybug")); mc.level.addEntity(namedBee);
            var unnamedBee=new Bee(beeType,mc.level); unnamedBee.setId(-60004);
            unnamedBee.setPos(8,67,17); mc.level.addEntity(unnamedBee);
            var slimeType=BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("slime"));
            for(int i=0;i<3;i++) {
                var slime=slimeType.create(mc.level,net.minecraft.world.entity.EntitySpawnReason.LOAD); slime.setId(-60005-i);
                slime.setPos(-80-i*.6,65,31); if(i==0) slime.setCustomName(Component.literal("Shyworm")); mc.level.addEntity(slime);
            }
            CritterSafariClient.apply(CritterSafariClient.settings);
        });
        context.waitTicks(2);
        context.runOnClient(mc -> {
            if(selected(-60001) || !selected(-60002)) throw new AssertionError("Ambient fish had ESP or named fish critter was lost: ambient="+selected(-60001)+", named="+selected(-60002)+", player="+mc.player.position()+", fish="+mc.level.getEntity(-60002)+", targets="+CritterSafariClient.tracker.targets());
            if(!selected(-60003) || !selected(-60004)) throw new AssertionError("Named observation did not resolve a fresh unnamed body by type and Forest biome: player="+mc.player.position()+", named="+selected(-60003)+", unnamed="+selected(-60004)+", biome="+SafariAreas.at(mc.level.getEntity(-60004).position()));
            CritterSafariClient.settings.oneCritterMode=true; CritterSafariClient.apply(CritterSafariClient.settings);
            CritterSafariClient.receiveSafariMessage(mc,"CAPTURE! You caught a Honeybug and gained a Honeybug Shard!");
            if(selected(-60004)) throw new AssertionError("Caught species kept its tracer on an unnamed inferred body");
            CritterSafariClient.run.toggle(SafariSpecies.HONEYBUG);
            if(!selected(-60004)) throw new AssertionError("Manual re-enable did not restore the inferred unnamed body");
            var worms=CritterSafariClient.tracker.targets().stream().filter(t -> t.name().equals("Shyworm") && t.parts().stream().anyMatch(e -> e.getId()==-60005)).toList();
            if(worms.size()!=1 || worms.getFirst().parts().size()!=3) throw new AssertionError("Named/inferred slime models did not form one Shyworm");
            CritterSafariClient.run.toggle(SafariSpecies.SHYWORM);
            if(CritterSafariClient.tracker.targets().stream().anyMatch(t -> t.parts().stream().anyMatch(e -> e.getId()<=-60005 && e.getId()>=-60007)))
                throw new AssertionError("Disabled Shyworm retained distant slime segment tracers");
            CritterSafariClient.run.reset(); CritterSafariClient.settings.oneCritterMode=false;
            for(int id:new int[]{-60001,-60002,-60003,-60004,-60005,-60006,-60007}) mc.level.removeEntity(id,Entity.RemovalReason.DISCARDED);
            CritterSafariClient.settings.detection="NAME"; CritterSafariClient.apply(CritterSafariClient.settings);
        });
    }
    private static com.mojang.authlib.GameProfile texturedProfile(String name,String hash) {
        String encoded=java.util.Base64.getEncoder().encodeToString(("{\"textures\":{\"SKIN\":{\"url\":\"http://textures.minecraft.net/texture/"+hash+"\"}}}").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return new GameProfile(UUID.randomUUID(),name,new com.mojang.authlib.properties.PropertyMap(
            com.google.common.collect.ImmutableMultimap.of("textures",new com.mojang.authlib.properties.Property("textures",encoded))));
    }
    private static void verifySpecialNpcAndMounds(ClientGameTestContext context,net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext singleplayer) {
        singleplayer.getServer().runCommand("tp @a -80 70 31");
        context.waitFor(mc -> mc.player.position().distanceToSqr(new Vec3(-80,70,31))<4);
        singleplayer.getClientLevel().waitForChunksRender();
        context.runOnClient(mc -> {
            CritterSafariClient.settings.detection="BOTH"; CritterSafariClient.settings.oneCritterMode=false;
            CritterSafariClient.run.reset();
            var p=mc.player.position();
            var hideyho=new RemotePlayer(mc.level,texturedProfile("tusg",NpcCritters.HIDEYHO_TEXTURE));
            hideyho.setId(-61001); hideyho.setPos(p.x+3,p.y,p.z+3); mc.level.addEntity(hideyho);
            var scrappy=new RemotePlayer(mc.level,new GameProfile(UUID.randomUUID(),"FoodNpc"));
            scrappy.setId(-61002); scrappy.setPos(p.x-4,p.y,p.z+3); mc.level.addEntity(scrappy);
            var plate=new ArmorStand((EntityType<? extends ArmorStand>)BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("armor_stand")),mc.level);
            plate.setId(-61003); plate.setPos(p.x-4,p.y+2,p.z+3); plate.setCustomName(Component.literal("Scrappy")); mc.level.addEntity(plate);
            for(int i=0;i<2;i++) {
                var display=new Display.ItemDisplay(BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("item_display")),mc.level);
                display.setId(-61004-i); display.setPos(p.x+i*4,p.y,p.z+6);
                var head=new ItemStack(Items.PLAYER_HEAD);
                head.set(DataComponents.PROFILE,net.minecraft.world.item.component.ResolvableProfile.createResolved(texturedProfile("mound",RockmiteMounds.TEXTURE)));
                display.getSlot(0).set(head);
                display.getEntityData().set(org.hypixelskyblockmods.crittersafariesp.mixin.DisplayAccessor.critterEsp$movementDuration(),i==0?0:3);
                mc.level.addEntity(display);
            }
            mc.player.setYRot(0); mc.player.setXRot(0); CritterSafariClient.apply(CritterSafariClient.settings);
        });
        context.waitTicks(2);
        context.runOnClient(mc -> {
            if(!selected(-61001) || !selected(-61002)) throw new AssertionError("Skin-identified Hideyho or nameplate-only Scrappy NPC was missed");
            if(!selected(-61004) || selected(-61005)) throw new AssertionError("Mound texture/duration failed to distinguish a real mound from an animated capture model");
            for(var target:CritterSafariClient.tracker.targets()) if(target.entity().getId()==-61001 || target.entity().getId()==-61002 || target.entity().getId()==-61004)
                if(!NpcCritters.interactionOnly(target)) throw new AssertionError("Interaction target incorrectly used capsule aiming");
            var p=mc.player.position(); mc.level.getEntity(-61001).setPos(p.x-3,p.y,p.z+8); CritterSafariClient.tracker.refresh();
        });
        context.waitTicks(2);
        context.takeScreenshot("purple-npc-teleport-and-amber-mound");
        context.runOnClient(mc -> {
            if(!selected(-61001)) throw new AssertionError("Hideyho lost its marker after teleporting without a nametag");
            CritterSafariClient.receiveSafariMessage(mc,"CAPTURE! You caught a Rockmite and gained a Rockmite Shard!");
            if(selected(-61004)) throw new AssertionError("Mounds stayed visible after a confirmed Rockmite catch");
            if(!CritterSafariClient.run.enabled(SafariSpecies.ROCKMITE)) throw new AssertionError("Mound completion auto-disabled actual critters while One Critter Mode was off");
            CritterSafariClient.run.reset();
            if(!selected(-61004)) throw new AssertionError("New run failed to restore Rockmite mounds");
            CritterSafariClient.run.toggle(SafariSpecies.ROCKMITE);
            if(selected(-61004)) throw new AssertionError("Manual Rockmite disable did not hide its mounds");
            CritterSafariClient.run.reset();
            for(int id:new int[]{-61001,-61002,-61003,-61004,-61005}) mc.level.removeEntity(id,Entity.RemovalReason.DISCARDED);
            CritterSafariClient.settings.detection="NAME"; CritterSafariClient.apply(CritterSafariClient.settings);
        });
    }
    private static void verifyGroupedModelsAndCrowd(ClientGameTestContext context) {
        context.runOnClient(mc -> {
            var p=mc.player.position(); CritterSafariClient.settings.detection="BOTH";
            var fishType=(EntityType<? extends Silverfish>)BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("silverfish"));
            for(int i=0;i<9;i++) {
                var segment=new Silverfish(fishType,mc.level);
                segment.setId(-40001-i); segment.setPos(p.x-3+(i<6?i:i-6)*.7,p.y+.25,p.z+(i<6?6:11));
                if(i==0 || i==6) segment.setCustomName(Component.literal("Shyworm"));
                mc.level.addEntity(segment);
            }
            var display=new Display.ItemDisplay(BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("item_display")),mc.level);
            display.setId(-40021); display.setPos(p.x+4,p.y+.5,p.z+7);
            display.getSlot(0).set(new ItemStack(Items.STONE)); display.setCustomName(Component.literal("Flitter")); mc.level.addEntity(display);
            var bat=new Bat((EntityType<? extends Bat>)BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("bat")),mc.level);
            bat.setId(-40022); bat.setPos(p.x+4,p.y+.3,p.z+7); mc.level.addEntity(bat);
            CritterSafariClient.tracker.refresh();
        });
        context.waitTicks(2);
        context.runOnClient(mc -> {
            var worms=CritterSafariClient.tracker.targets().stream().filter(target -> target.name().equals("Shyworm")).toList();
            if(worms.size()!=2 || worms.stream().mapToInt(target -> target.parts().size()).sum()!=9)
                throw new AssertionError("Shyworm segments were not grouped into two separate critters");
            var flitters=CritterSafariClient.tracker.targets().stream().filter(target -> target.name().equals("Flitter")).toList();
            if(flitters.size()!=1 || flitters.getFirst().parts().size()!=2 || flitters.getFirst().entity().getId()!=-40021)
                throw new AssertionError("Duplicate Flitter models did not use one larger box");
        });
        context.takeScreenshot("grouped-shyworms-and-flitter");
        context.runOnClient(mc -> {
            for(int i=0;i<9;i++) mc.level.removeEntity(-40001-i,Entity.RemovalReason.DISCARDED);
            for(int id:new int[]{-40021,-40022}) mc.level.removeEntity(id,Entity.RemovalReason.DISCARDED);
            var p=mc.player.position();
            var beeType=(EntityType<? extends Bee>)BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("bee"));
            for(int i=0;i<80;i++) {
                var bee=new Bee(beeType,mc.level); bee.setId(-50001-i);
                bee.setPos(p.x-12+(i%10)*2.5,p.y+.5+(i/10)*.35,p.z+10+(i/10)*2);
                bee.setCustomName(Component.literal("Honeybug")); mc.level.addEntity(bee);
            }
            CritterSafariClient.settings.aimNames=false; CritterSafariClient.tracker.refresh();
        });
        context.waitTicks(3);
        context.runOnClient(mc -> {
            if(CritterSafariClient.tracker.targets().size()<80 || OverlayQuad.lastQuadCount<1000 || OverlayQuad.lastSubmissionCount!=1)
                throw new AssertionError("Crowded overlay was not drawn as one smooth geometry batch");
            CritterSafariClient.LOGGER.info("Crowded local scene: {} targets, {} quads, {} geometry submission",
                CritterSafariClient.tracker.targets().size(),OverlayQuad.lastQuadCount,OverlayQuad.lastSubmissionCount);
            var bee=mc.level.getEntity(-50001); bee.setPos(bee.position().add(.15,0,0));
        });
        context.waitTicks(1);
        context.takeScreenshot("crowded-batched-overlay");
        context.runOnClient(mc -> {
            for(int i=0;i<80;i++) mc.level.removeEntity(-50001-i,Entity.RemovalReason.DISCARDED);
            CritterSafariClient.settings.aimNames=true; CritterSafariClient.settings.detection="NAME"; CritterSafariClient.tracker.refresh();
        });
        context.waitTicks(1);
    }
    private static java.lang.reflect.Field radiusField() {
        try { return SettingsScreen.General.class.getField("radius"); } catch(NoSuchFieldException e) { throw new AssertionError(e); }
    }
    private static void verifyFloorDropsAndModelBounds(ClientGameTestContext context) {
        context.runOnClient(mc -> {
            var p=mc.player.position();
            var drop=BlockPos.containing(p.x+3,p.y,p.z+5);
            for(int i=0;i<3;i++) {
                var display=new Display.ItemDisplay(BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("item_display")),mc.level);
                display.setId(-30001-i); display.setPos(drop.getX()+.25+i*.15,drop.getY()+.1,drop.getZ()+.5);
                display.getSlot(0).set(new ItemStack(Items.STRING)); mc.level.addEntity(display);
            }
            var model=new Display.ItemDisplay(BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("item_display")),mc.level);
            model.setId(-30004); model.setPos(p.x-7,p.y+.5,p.z+5);
            model.getSlot(0).set(new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace("green_shulker_box"))));
            try {
                var method=Display.class.getDeclaredMethod("setTransformation",Transformation.class); method.setAccessible(true);
                method.invoke(model,new Transformation(new Vector3f(3,0,0),new Quaternionf(),new Vector3f(1,1,1),new Quaternionf()));
            } catch(ReflectiveOperationException e) { throw new AssertionError(e); }
            mc.level.addEntity(model); CritterSafariClient.tracker.refresh();
        });
        context.waitTicks(2);
        context.runOnClient(mc -> {
            var p=mc.player.position();
            var drop=BlockPos.containing(p.x+3,p.y,p.z+5);
            var packet=new ClientboundLevelParticlesPacket(ParticleTypes.HAPPY_VILLAGER,false,false,drop.getX()+.5,drop.getY()+1.1,drop.getZ()+.5,0,0,0,0,1);
            if(!FloorDrops.nearby(mc,CritterSafariClient.settings).isEmpty()) throw new AssertionError("Strings alone incorrectly confirmed a floor drop");
            mc.getConnection().handleParticleEvent(packet); // Exercises the production packet hook on the local connection.
            if(FloorDrops.nearby(mc,CritterSafariClient.settings).size()!=1) throw new AssertionError("Three-string green-particle floor drop was not detected");
            if(!selected(-30004)) throw new AssertionError("Unnamed green shulker display critter was missed");
            var model=mc.level.getEntity(-30004);
            if(Math.abs(ModelBounds.of(model,1).getCenter().x-(model.getX()+3))>.01)
                throw new AssertionError("Display box ignored its visible model's translation");
        });
        context.takeScreenshot("floor-drop-and-offset-model");
        context.waitTicks(140);
        context.runOnClient(mc -> {
            if(FloorDrops.nearby(mc,CritterSafariClient.settings).size()!=1) throw new AssertionError("Floor-drop box expired while its three displays still existed");
        });
        context.runOnClient(mc -> {
            CritterSafariClient.settings.radius=2; CritterSafariClient.tracker.refresh();
            if(!FloorDrops.nearby(mc,CritterSafariClient.settings).isEmpty()) throw new AssertionError("Floor-drop box bypassed radius");
            CritterSafariClient.settings.radius=100;
            mc.level.removeEntity(-30001,Entity.RemovalReason.DISCARDED);
            if(!FloorDrops.nearby(mc,CritterSafariClient.settings).isEmpty()) throw new AssertionError("Collected floor drop kept its box");
            for(int id:new int[]{-30002,-30003,-30004}) mc.level.removeEntity(id,Entity.RemovalReason.DISCARDED);
            CritterSafariClient.tracker.refresh();
        });
        context.waitTicks(1);
    }
}
