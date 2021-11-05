package com.github.eterdelta.broomsmod.entity;

import com.github.eterdelta.broomsmod.registry.BroomsEnchantments;
import com.github.eterdelta.broomsmod.registry.BroomsEntities;
import com.github.eterdelta.broomsmod.registry.BroomsItems;
import com.github.eterdelta.broomsmod.registry.BroomsSounds;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.passive.WaterMobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

import java.util.List;

public class WoodenBroomEntity extends Entity {
    private static final DataParameter<ItemStack> ITEM = EntityDataManager.defineId(WoodenBroomEntity.class, DataSerializers.ITEM_STACK);
    private static final DataParameter<Integer> HURT_TIME = EntityDataManager.defineId(WoodenBroomEntity.class, DataSerializers.INT);
    private static final DataParameter<Integer> HURT_DIR = EntityDataManager.defineId(WoodenBroomEntity.class, DataSerializers.INT);
    private static final DataParameter<Float> DAMAGE = EntityDataManager.defineId(WoodenBroomEntity.class, DataSerializers.FLOAT);
    public int hoverTime;
    public boolean canHover;
    public boolean seaBreezing;
    protected boolean inputLeft;
    protected boolean inputRight;
    protected boolean inputUp;
    protected boolean inputDown;
    protected boolean inputJump;
    private int lerpSteps;
    private double lerpX;
    private double lerpY;
    private double lerpZ;
    private double lerpYRot;
    private double lerpXRot;

    public WoodenBroomEntity(EntityType<? extends WoodenBroomEntity> entityType, World level) {
        super(entityType, level);
        this.blocksBuilding = true;
        this.canHover = true;
        this.hoverTime = this.getMaxHoverTime();
    }

    public WoodenBroomEntity(ItemStack itemStack, World level, double x, double y, double z) {
        this(BroomsEntities.WOODEN_BROOM.get(), level);
        this.setItem(itemStack.copy());
        this.setPos(x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(ITEM, ItemStack.EMPTY);
        this.entityData.define(HURT_TIME, 0);
        this.entityData.define(HURT_DIR, 1);
        this.entityData.define(DAMAGE, 0.0F);
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    @Override
    public double getPassengersRidingOffset() {
        return 0.1D;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        } else if (!this.level.isClientSide && this.isAlive()) {
            this.setHurtDir(-this.getHurtDir());
            this.setHurtTime(10);
            this.setDamage(this.getDamage() + amount * 10.0F);
            this.markHurt();
            boolean flag = source.getEntity() instanceof PlayerEntity && ((PlayerEntity) source.getEntity()).abilities.instabuild;
            if (flag || this.getDamage() > 40.0F) {
                if (!flag && this.level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
                    this.spawnBroomItem();
                }
                this.remove();
            }
            return true;
        } else {
            return true;
        }
    }

    @Override
    public void push(Entity entity) {
        if (entity instanceof WoodenBroomEntity) {
            if (entity.getBoundingBox().minY < this.getBoundingBox().maxY) {
                super.push(entity);
            }
        } else if (entity.getBoundingBox().minY <= this.getBoundingBox().minY) {
            super.push(entity);
        }
    }

    @Override
    public void animateHurt() {
        this.setHurtDir(-this.getHurtDir());
        this.setHurtTime(10);
        this.setDamage(this.getDamage() * 11.0F);
    }

    @Override
    public boolean isPickable() {
        return this.isAlive();
    }

    @Override
    public void lerpTo(double p_38299_, double p_38300_, double p_38301_, float p_38302_, float p_38303_, int p_38304_, boolean p_38305_) {
        this.lerpX = p_38299_;
        this.lerpY = p_38300_;
        this.lerpZ = p_38301_;
        this.lerpYRot = p_38302_;
        this.lerpXRot = p_38303_;
        this.lerpSteps = p_38304_;
    }

    @Override
    public Direction getMotionDirection() {
        return this.getDirection().getClockWise();
    }

    @Override
    public void tick() {
        super.tick();
        this.tickLerp();

        /* onGround check for client is movement dependent, so it will return
         * true every time the broom reaches 0.0 speed in the air.
         * This speed tweak smooths the broom movements and solves the problem.
         */
        this.setDeltaMovement(this.getDeltaMovement().multiply(0.8D, 0.9D, 0.8D));

        if (!this.isOnGround() && !this.seaBreezing) {
            if (this.hoverTime > 0) {
                this.canHover = true;
                this.hoverTime--;

                if (this.hoverTime == 5) {
                    this.playSound(BroomsSounds.BROOM_FALL.get(), 1.0F, 1.0F);
                }
            } else {
                this.canHover = false;
            }
        } else if (this.hoverTime != this.getMaxHoverTime()) {
            this.canHover = true;
            this.hoverTime = this.getMaxHoverTime();
        }

        if (!this.isNoGravity()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.03D, 0.0D));
        }

        if (this.getHurtTime() > 0) {
            this.setHurtTime(this.getHurtTime() - 1);
        }

        if (this.getDamage() > 0.0F) {
            this.setDamage(this.getDamage() - 1.0F);
        }

        if (this.isVehicle()) {
            LivingEntity passenger = (LivingEntity) this.getControllingPassenger();
            this.yRot = passenger.yRot;
        }

        if (this.isControlledByLocalInstance()) {
            if (this.level.isClientSide) {
                this.handleInputs();
            }
            this.move(MoverType.SELF, this.getDeltaMovement());
        } else {
            this.setDeltaMovement(Vector3d.ZERO);
        }

        if (this.level.getBlockState(this.blockPosition().below()).is(Blocks.WATER) && EnchantmentHelper.getItemEnchantmentLevel(BroomsEnchantments.SEA_BREEZE.get(), this.getItem()) > 0) {
            this.seaBreezing = true;
            if (this.getDeltaMovement().y() < 0) {
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.9D, 0.3D, 0.9D));
            }
        } else {
            this.seaBreezing = false;
        }

        this.checkInsideBlocks();
        List<Entity> list = this.level.getEntities(this, this.getBoundingBox().inflate(0.2F, -0.01F, 0.2F), EntityPredicates.pushableBy(this));
        if (!list.isEmpty()) {
            boolean flag = !this.level.isClientSide && !(this.getControllingPassenger() instanceof PlayerEntity);

            for (Entity entity : list) {
                if (!entity.hasPassenger(this)) {
                    if (flag && this.getPassengers().size() < 2 && !entity.isPassenger() && entity.getBbWidth() < this.getBbWidth() && entity instanceof LivingEntity && !(entity instanceof WaterMobEntity) && !(entity instanceof PlayerEntity)) {
                        entity.startRiding(this);
                    } else {
                        this.push(entity);
                    }
                }
            }
        }

        if (this.isInWaterOrBubble()) {
            this.playSound(BroomsSounds.BROOM_DESTROY.get(), 0.8F, 1.0F);
            this.spawnBroomItem();
            this.remove();
        }
    }

    @Override
    public void turn(double p_195049_1_, double p_195049_3_) {
    }

    private void tickLerp() {
        if (this.isControlledByLocalInstance()) {
            this.lerpSteps = 0;
            this.setPacketCoordinates(this.getX(), this.getY(), this.getZ());
        }

        if (this.lerpSteps > 0) {
            double stepX = this.getX() + (this.lerpX - this.getX()) / (double) this.lerpSteps;
            double stepY = this.getY() + (this.lerpY - this.getY()) / (double) this.lerpSteps;
            double stepZ = this.getZ() + (this.lerpZ - this.getZ()) / (double) this.lerpSteps;
            this.yRot = (float) (this.yRot + MathHelper.wrapDegrees(this.lerpYRot - this.yRot) / this.lerpSteps);
            this.xRot = this.xRot + (float) (this.lerpXRot - (double) this.xRot) / (float) this.lerpSteps;

            --this.lerpSteps;
            this.setPos(stepX, stepY, stepZ);
            this.setRot(this.yRot, this.xRot);
        }
    }

    private void handleInputs() {
        if (this.isVehicle()) {
            LivingEntity controller = (LivingEntity) this.getControllingPassenger();
            Vector3d inputVector = this.getInputVector(new Vector3d(controller.xxa * 0.8F, 0.0D, controller.zza), this.getSpeed(), this.yRot);

            if (this.inputLeft || this.inputRight || this.inputUp || this.inputDown) {
                this.setDeltaMovement(this.getDeltaMovement().add(inputVector));
            }

            if (this.inputJump && this.canHover) {
                this.setDeltaMovement(this.getDeltaMovement().add(0.0D, this.getHoverSpeed(), 0.0D));
            }
        }
    }

    @Override
    public void positionRider(Entity rider) {
        super.positionRider(rider);
        if (rider instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) rider;
            player.yBodyRot = this.yRotO;
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundNBT compoundTag) {
        compoundTag.put("Item", this.getItem().save(new CompoundNBT()));
    }

    @Override
    protected void readAdditionalSaveData(CompoundNBT compoundTag) {
        CompoundNBT itemTag = compoundTag.getCompound("Item");
        this.setItem(ItemStack.of(itemTag));
    }


    @Override
    public ActionResultType interact(PlayerEntity p_38330_, Hand p_38331_) {
        if (p_38330_.isSecondaryUseActive()) {
            return ActionResultType.PASS;
        } else {
            if (!this.level.isClientSide()) {
                return p_38330_.startRiding(this) ? ActionResultType.CONSUME : ActionResultType.PASS;
            } else {
                return ActionResultType.SUCCESS;
            }
        }
    }

    @Override
    protected void checkFallDamage(double p_19911_, boolean p_19912_, BlockState p_19913_, BlockPos p_19914_) {
        this.fallDistance = 0.0F;
    }

    @Override
    public Entity getControllingPassenger() {
        List<Entity> list = this.getPassengers();
        return list.isEmpty() ? null : list.get(0);
    }

    public void setInputs(boolean left, boolean right, boolean up, boolean down, boolean jumping) {
        this.inputLeft = left;
        this.inputRight = right;
        this.inputUp = up;
        this.inputDown = down;
        this.inputJump = jumping;
    }

    @Override
    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);
        if (this.isControlledByLocalInstance() && this.lerpSteps > 0) {
            this.lerpSteps = 0;
            this.absMoveTo(this.lerpX, this.lerpY, this.lerpZ, (float) this.lerpYRot, (float) this.lerpXRot);
        }
    }

    public Vector3d getInputVector(Vector3d movement, float speed, float angle) {
        double length = movement.lengthSqr();
        if (length < 1.0E-7D) {
            return Vector3d.ZERO;
        } else {
            Vector3d vec3 = (length > 1.0D ? movement.normalize() : movement).scale(speed);
            float f = MathHelper.sin(angle * ((float) Math.PI / 180F));
            float f1 = MathHelper.cos(angle * ((float) Math.PI / 180F));
            return new Vector3d(vec3.x * (double) f1 - vec3.z * (double) f, vec3.y, vec3.z * (double) f1 + vec3.x * (double) f);
        }
    }

    public void spawnBroomItem() {
        if (!this.getItem().isEmpty()) {
            this.spawnAtLocation(this.getItem());
        } else {
            this.spawnAtLocation(new ItemStack(BroomsItems.WOODEN_BROOM.get()));
        }
    }

    public float getDamage() {
        return this.entityData.get(DAMAGE);
    }

    public void setDamage(float p_38312_) {
        this.entityData.set(DAMAGE, p_38312_);
    }

    public int getHurtTime() {
        return this.entityData.get(HURT_TIME);
    }

    public void setHurtTime(int p_38355_) {
        this.entityData.set(HURT_TIME, p_38355_);
    }

    public int getHurtDir() {
        return this.entityData.get(HURT_DIR);
    }

    public void setHurtDir(int p_38363_) {
        this.entityData.set(HURT_DIR, p_38363_);
    }

    public ItemStack getItem() {
        return this.entityData.get(ITEM);
    }

    public void setItem(ItemStack itemStack) {
        this.entityData.set(ITEM, itemStack);
    }

    public double getHoverSpeed() {
        return 0.09D;
    }

    public float getSpeed() {
        if (this.isOnGround()) {
            return 0.08F + (0.08F * (EnchantmentHelper.getItemEnchantmentLevel(BroomsEnchantments.LAND_SKILLS.get(), this.getItem()) * 20 / 100.0F));
        } else {
            return 0.08F + (0.08F * (EnchantmentHelper.getItemEnchantmentLevel(BroomsEnchantments.AIR_SKILLS.get(), this.getItem()) * 20 / 100.0F));
        }
    }

    public int getMaxHoverTime() {
        return (int) (100 + (100 * (EnchantmentHelper.getItemEnchantmentLevel(BroomsEnchantments.HOVERING.get(), this.getItem()) * 25 / 100.0F)));
    }
}