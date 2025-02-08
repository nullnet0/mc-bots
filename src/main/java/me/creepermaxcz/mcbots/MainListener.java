package me.creepermaxcz.mcbots;

import org.geysermc.mcprotocollib.network.ProxyInfo;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.*;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundPlayerChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;

import java.net.InetSocketAddress;
import java.net.Proxy;

public class MainListener implements SessionListener {

    public MainListener(String nickname) {
        Log.info("MainListener registered for: " + nickname);
    }

    @Override
    public void packetReceived(Session session, Packet packet) {
        // From 1.19.1 (as well as 1.19), the class ClientboundChatPacket was removed.
        // Instead, they use ClientboundPlayerChatPacket and ClientboundSystemChatPacket for taking care of chat packets.
        Component message = null;
        Component sender;
        boolean chatPrintedOut = false;

        if (packet instanceof ClientboundPlayerChatPacket) {
            ClientboundPlayerChatPacket clientboundPlayerChatPacket = ((ClientboundPlayerChatPacket) packet);
            message = clientboundPlayerChatPacket.getUnsignedContent();
            sender = clientboundPlayerChatPacket.getName();

            // Sometimes the message's body gets null.
            // For example, some commands like /say makes the message content as null.
            // However, the message exists as in getMessagePlain(), thus can retrieve message using the method.
            if (message == null) {
                Log.chat(Utils.getFullText((TextComponent) sender, Component.text(clientboundPlayerChatPacket.getContent()), Main.coloredChat));
            } else {
                Log.chat(Utils.getFullText((TextComponent) sender, (TextComponent) message, Main.coloredChat));
            }
            chatPrintedOut = true;

        } else if (packet instanceof ClientboundSystemChatPacket) { // When this was SystemChat packet.
            message = ((ClientboundSystemChatPacket) packet).getContent();
        }

        // For output of commands, this is the case where this program prints out the message to user.
        if (message instanceof TextComponent && !chatPrintedOut) {
            TextComponent msg = (TextComponent) message;
            Log.chat(Utils.getFullText(msg, Main.coloredChat));
        }

        if (message instanceof TranslatableComponent) {
            TranslatableComponent msg = (TranslatableComponent) message;
            Log.chat("[T]", Utils.translate(msg));
        }
    }

    @Override
    public void packetSending(PacketSendingEvent event) {

    }

    @Override
    public void packetSent(Session session, Packet packet) {

    }

    @Override
    public void packetError(PacketErrorEvent event) {

    }

    @Override
    public void connected(ConnectedEvent event) {

    }

    @Override
    public void disconnecting(DisconnectingEvent event) {

    }

    @Override
    public void disconnected(DisconnectedEvent event) {
        if (Main.autoReconnect) {
            try {
                Bot disconnectedBot = Main.bots.keySet().stream()
                        .filter(bot -> !bot.isConnected())
                        .findFirst()
                        .orElse(null);

                if (disconnectedBot != null) {
                    Bot newBot = null;
                    ProxyInfo proxyInfo = null;
                    if (Main.autoReconnectWithSameProxy) {
                        proxyInfo = Main.bots.get(disconnectedBot);
                        newBot = new Bot(
                                disconnectedBot.getName(),
                                new InetSocketAddress(event.getSession().getHost(), event.getSession().getPort()),
                                proxyInfo
                        );
                    } else {
                        InetSocketAddress proxySocket = Main.proxies.get(Main.proxyIndex);

                        if (!Main.minimal) {
                            Log.info(
                                    "Using proxy: (" + Main.proxyIndex + ")",
                                    proxySocket.getHostString() + ":" + proxySocket.getPort()
                            );
                        }

                        proxyInfo = new ProxyInfo(
                                Main.proxyType,
                                proxySocket
                        );

                        //increment or reset current proxy index
                        if (Main.proxyIndex < (Main.proxyCount - 1)) {
                            Main.proxyIndex++;
                        } else {
                            Main.proxyIndex = 0;
                        }
                    }
                    newBot.start();

                    Main.bots.remove(disconnectedBot);
                    Main.bots.put(newBot, proxyInfo);

                    newBot.registerMainListener();
                    Log.info("Bot reconnected successfully.");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
