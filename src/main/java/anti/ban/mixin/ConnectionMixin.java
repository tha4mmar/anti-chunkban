package anti.ban.mixin;

import anti.ban.Saver;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public class ConnectionMixin {

    // Server-side (and client-side, harmlessly) safety net: when the vanilla
    // encoder refuses to send/accept a packet because it is oversized
    // ("Packet too large..." / "Badly compressed packet..."), vanilla's default
    // reaction is to tear down the whole connection, which is exactly the
    // "server side chunkban" case this mod can't currently prevent (see README).
    //
    // Instead of letting that one oversized packet kill the connection, we log
    // it and drop the packet, keeping the player connected. This mirrors what
    // the client-side decoder mixins already do for malformed incoming data,
    // but on the sending/encoding path.

    @Inject(method = "exceptionCaught", at = @At("HEAD"), cancellable = true)
    private void absorbOversizedPacket(ChannelHandlerContext ctx, Throwable exception, CallbackInfo ci) {
        if (!Saver.chunkban) {
            return;
        }

        Throwable cause = exception;
        while (cause != null) {
            String message = cause.getMessage();
            if (message != null && (message.contains("Packet too large") || message.contains("Badly compressed packet"))) {
                System.out.println("[anti-chunkban] Swallowed oversized packet instead of disconnecting: " + message);
                ci.cancel();
                return;
            }
            cause = cause.getCause();
        }
    }
}
