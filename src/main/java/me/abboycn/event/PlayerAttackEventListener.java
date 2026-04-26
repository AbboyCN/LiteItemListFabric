package me.abboycn.event;

import me.abboycn.LiteItemListFabric;
import me.abboycn.executor.CMDList;
import me.abboycn.gui.TaskManagerScreenHandler;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;

public class PlayerAttackEventListener {
    public static void register(){
        AttackBlockCallback.EVENT.register(((player, world, hand, pos, direction) -> {
            if(!(player.isPlayer()&&player.isHolding(Items.FIREWORK_ROCKET)&&player.isSneaking())){return ActionResult.PASS;}
            if(LiteItemListFabric.taskManager.getTaskByPlayer((ServerPlayerEntity) player)==null){
                TaskManagerScreenHandler.openTaskManagerMenu((ServerPlayerEntity) player);
            }
            else {
                CMDList.showGUI((ServerPlayerEntity) player);
            }
            return ActionResult.PASS;
        }));
    }
}
