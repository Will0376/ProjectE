package moze_intel.projecte.gameObjs.items;

import moze_intel.projecte.api.item.IProjectileShooter;
import moze_intel.projecte.gameObjs.ObjHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;

import javax.annotation.Nonnull;

public class CataliticLens extends DestructionCatalyst implements IProjectileShooter
{
	public CataliticLens() 
	{
		super("catalitic_lens", (byte)7);
		this.setNoRepair();
	}
	
	@Override
	public boolean shootProjectile(@Nonnull EntityPlayer player, @Nonnull ItemStack stack, EnumHand hand)
	{
		return ((IProjectileShooter) ObjHandler.hyperLens).shootProjectile(player, stack, hand);
	}
}
