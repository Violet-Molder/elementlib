package com.linweiyun.elementlib.core.system.attack;

import com.linweiyun.elementlib.core.module.ElibModuleHost;
import com.linweiyun.elementlib.core.module.ElibModuleQuery;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 攻击几何 —— 把「从哪出手、朝哪、多远」换算成一次模块宿主收集。
 *
 * <p>盒子膨胀、体积上限、跳过攻击者自己；方块由 {@link ElibModuleQuery} 连同实体一起收。
 */
public final class ElibAttackGeometry {

    /** 扫描盒膨胀（格）。 */
    public static final double INFLATE = 1.0;

    /** 扫描体上限，防止某个招式把攻击距离配成离谱的值时遍历爆炸。 */
    public static final int MAX_VOLUME = 4096;

    private ElibAttackGeometry() {
    }

    public static List<ElibModuleHost> collect(ServerLevel level, @Nullable Entity attacker,
                                               Vec3 origin, Vec3 direction, double reach,
                                               @Nullable ElibModuleQuery.BlockInterest blockInterest) {
        List<ElibModuleHost> hosts = new ArrayList<>();
        if (level == null || origin == null || direction == null) {
            return hosts;
        }
        Vec3 normalized = direction.lengthSqr() < 1.0E-6 ? Vec3.ZERO : direction.normalize();
        AABB box = new AABB(origin, origin.add(normalized.scale(Math.max(0.0, reach))))
                .inflate(INFLATE);
        String attackerKey = attacker == null ? null : "entity:" + attacker.getUUID();
        for (ElibModuleHost host : ElibModuleQuery.in(level, box, blockInterest)) {
            if (host.blockPos() == null) {
                continue;
            }
            if (attackerKey != null && attackerKey.equals(host.hostKey())) {
                continue;
            }
            hosts.add(host);
        }
        return hosts;
    }
}
