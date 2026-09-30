package com.linweiyun.elementlib.util.log;

import lombok.Getter;
import lombok.Setter;

/** 日志分组：每组一个独立的开关，{@link ModLog} 按组决定这条日志出不出声。 */
public enum LogGroup {
   COMBAT("战斗（动作 / 伤害 / 目标选择）"),
   ELEMENT("元素（附着 / 反应）"),
   CHARACTER("角色（角色 / 角色效果）"),
   CONTENT("内容（物品 / 实体 / 技能节点）"),
   RENDER("渲染（几何 / 资源 / 界面 / 飘字）"),
   ANIMATION("动画（状态切换 / 播放起止）"),
   CORE("核心服务（网络 / 存档 / 状态 / 资源包 / 刷怪 / 卡池 / 音效）"),
   MIXIN("混入（Mixin）");

   private final String label;
   @Getter
   @Setter
   private volatile boolean enabled = true;

   LogGroup(String label) {
      this.label = label;
   }

   public String label() {
      return this.label;
   }

}