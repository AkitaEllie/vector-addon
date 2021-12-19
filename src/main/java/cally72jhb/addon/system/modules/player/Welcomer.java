package cally72jhb.addon.system.modules.player;

import cally72jhb.addon.VectorAddon;
import it.unimi.dsi.fastutil.chars.Char2CharArrayMap;
import it.unimi.dsi.fastutil.chars.Char2CharMap;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Welcomer extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgWelcome = settings.createGroup("Welcome");
    private final SettingGroup sgGoodbye = settings.createGroup("Goodbye");

    private final Setting<Boolean> ignoreFriends = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-friends")
        .description("Ignores friended players.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> smallCaps = sgGeneral.add(new BoolSetting.Builder()
        .name("small-caps")
        .description("Sends all messages with small caps.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> randomMsg = sgGeneral.add(new BoolSetting.Builder()
        .name("random")
        .description("Sends random messages every kill or pop.")
        .defaultValue(true)
        .build()
    );

    // Welcome

    private final Setting<Boolean> welcome = sgWelcome.add(new BoolSetting.Builder()
        .name("welcome")
        .description("Sends messages in the chat when a player joins.")
        .defaultValue(true)
        .build()
    );

    private final Setting<String> welcomeString = sgWelcome.add(new StringSetting.Builder()
        .name("welcome-message")
        .description("The message to send when a player joins.")
        .defaultValue("welcome {player}")
        .visible(() -> !randomMsg.get() && welcome.get())
        .build()
    );

    private final Setting<List<String>> welcomeMessages = sgWelcome.add(new StringListSetting.Builder()
        .name("welcome-messages")
        .description("The random messages to send when a player joins.")
        .defaultValue(List.of("welcome {player}", "hello"))
        .visible(() -> randomMsg.get() && welcome.get())
        .build()
    );

    private final Setting<Integer> welcomeDelay = sgWelcome.add(new IntSetting.Builder()
        .name("welcome-delay")
        .description("How long to wait in ticks before sending another welcome message.")
        .defaultValue(20)
        .min(0)
        .visible(welcome::get)
        .build()
    );

    // Goodbype

    private final Setting<Boolean> bye = sgGoodbye.add(new BoolSetting.Builder()
        .name("goodbye")
        .description("Sends messages in the chat when a player joins.")
        .defaultValue(true)
        .build()
    );

    private final Setting<String> byeString = sgGoodbye.add(new StringSetting.Builder()
        .name("bye-message")
        .description("The message to send when a player joins.")
        .defaultValue("welcome {player}")
        .visible(() -> !randomMsg.get() && bye.get())
        .build()
    );

    private final Setting<List<String>> byeMessages = sgGoodbye.add(new StringListSetting.Builder()
        .name("bye-messages")
        .description("The random messages to send when a player joins.")
        .defaultValue(List.of("welcome {player}", "hello"))
        .visible(() -> randomMsg.get() && bye.get())
        .build()
    );

    private final Setting<Integer> byeDelay = sgGoodbye.add(new IntSetting.Builder()
        .name("bye-delay")
        .description("How long to wait in ticks before sending another welcome message.")
        .defaultValue(20)
        .min(0)
        .visible(bye::get)
        .build()
    );

    private final Char2CharMap SMALL_CAPS = new Char2CharArrayMap();

    private List<PlayerListS2CPacket.Entry> prevEntries;
    private List<PlayerListS2CPacket.Entry> entries;
    private Random random;
    private int wTimer;
    private int bTimer;

    public Welcomer() {
        super(VectorAddon.CATEGORY, "welcomer", "Send a chat message when a player joins.");

        String[] a = "abcdefghijklmnopqrstuvwxyz".split("");
        String[] b = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘqʀꜱᴛᴜᴠᴡxʏᴢ".split("");
        for (int i = 0; i < a.length; i++) SMALL_CAPS.put(a[i].charAt(0), b[i].charAt(0));
    }

    @Override
    public void onActivate() {
        prevEntries = new ArrayList<>();
        random = new Random();
        wTimer = 0;
        bTimer = 0;
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (!(event.packet instanceof PlayerListS2CPacket packet)) return;
        if (packet.getAction() != PlayerListS2CPacket.Action.ADD_PLAYER && packet.getAction() != PlayerListS2CPacket.Action.REMOVE_PLAYER) return;

        entries = packet.getEntries();

        if (packet.getAction() == PlayerListS2CPacket.Action.ADD_PLAYER) {
            for (PlayerListS2CPacket.Entry entry : entries) {
                if (entry != null && entry.getProfile() != null && entry.getDisplayName() != null && welcome.get()) {
                    String name = entry.getDisplayName().asString();

                    boolean existed = true;

                    for (PlayerListS2CPacket.Entry prevEntry : prevEntries) {
                        if (prevEntry != null && prevEntry.getDisplayName() != null
                            && entry.getDisplayName().asString().equals(prevEntry.getDisplayName().asString())) existed = false;
                    }

                    if ((Friends.get().get(name) != null && ignoreFriends.get()) && existed) {
                        info(apply(name, randomMsg.get() ? welcomeMessages.get() : List.of(welcomeString.get())));
                    }
                }
            }
        }

        if (packet.getAction() == PlayerListS2CPacket.Action.REMOVE_PLAYER) {
            for (PlayerListS2CPacket.Entry entry : entries) {
                if (entry != null && entry.getProfile() != null && entry.getDisplayName() != null && bye.get()) {
                    String name = entry.getDisplayName().asString();

                    boolean existed = true;

                    for (PlayerListS2CPacket.Entry prevEntry : prevEntries) {
                        if (prevEntry != null && prevEntry.getDisplayName() != null
                            && entry.getDisplayName().asString().equals(prevEntry.getDisplayName().asString())) existed = false;
                    }

                    if ((Friends.get().get(name) != null && ignoreFriends.get()) && existed) {
                        info(apply(name, randomMsg.get() ? byeMessages.get() : List.of(byeString.get())));
                    }
                }
            }
        }

        prevEntries = packet.getEntries();
    }

    @EventHandler
    private void onPostTick(TickEvent.Post event) {
        wTimer++;
        bTimer++;
    }

    // Messaging

    private void sendMsg(String string) {
        StringBuilder sb = new StringBuilder();

        if (smallCaps.get()) {
            for (char ch : string.toCharArray()) {
                if (SMALL_CAPS.containsKey(ch)) sb.append(SMALL_CAPS.get(ch));
                else sb.append(ch);
            }
        } else {
            sb.append(string);
        }

        mc.player.sendChatMessage(sb.toString());
    }

    // Utils

    private String apply(String player, List<String> strings) {
        return strings.get(random.nextInt(strings.size())).replace("{player}", player);
    }
}