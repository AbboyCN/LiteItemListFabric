package me.abboycn.event;

import me.abboycn.executor.CMDList;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;

public class PlayerAttackEventListener {
    public static void register(){
        AttackBlockCallback.EVENT.register(((player, world, hand, pos, direction) -> {
            if(!(player.isPlayer()&&player.isSneaking()&&player.isHolding(Items.FIREWORK_ROCKET))){return ActionResult.PASS;}
            CMDList.showGUI((ServerPlayerEntity) player);
            return ActionResult.PASS;
        }));
    }
}
