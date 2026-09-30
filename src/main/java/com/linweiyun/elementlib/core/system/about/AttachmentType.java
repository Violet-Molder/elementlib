package com.linweiyun.elementlib.core.system.about;

/**
 * 附着档次枚举 —— 技能填 {@code elementAmount()} 时的取值来源。
 */
public enum AttachmentType {
  WEAK(AttachmentProfile.WEAK),
  STRONG(AttachmentProfile.STRONG),
  ULTRA_STRONG(AttachmentProfile.ULTRA_STRONG);

  private final AttachmentProfile profile;

  AttachmentType(AttachmentProfile profile) {
    this.profile = profile;
  }

  /** 损耗前的初始附着量（U）—— 技能填进 {@code elementAmount()} 的值。 */
  public float getInitialAmount() {
    return profile.getBaseQuantity();
  }

  /** 每秒衰减速率（U/s；1 秒 = 20 tick）。 */
  public float getDecayPer20Ticks() {
    return profile.getDecayPerSecond();
  }

  /** 这个档次对应的附着参数。 */
  public AttachmentProfile getProfile() {
    return profile;
  }
}
