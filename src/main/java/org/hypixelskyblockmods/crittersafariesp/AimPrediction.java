package org.hypixelskyblockmods.crittersafariesp;

import net.minecraft.world.phys.Vec3;

/** Solve the first reachable intercept using the same discrete drag/gravity steps as the preview. */
public final class AimPrediction {
    public record Aim(Vec3 direction, Vec3 point, double ticks) {}
    private record Travel(double factor, double drop) {}
    private record TravelTable(double[] factors,double[] drops,double[] scales) {
        Travel at(double time) {
            int whole=(int)time; double fraction=time-whole;
            return new Travel(factors[whole]+(factors[whole+1]-factors[whole])*fraction,
                drops[whole]+(drops[whole+1]-drops[whole])*fraction);
        }
    }
    private AimPrediction() {}
    public static Aim solve(Vec3 start,Vec3 target,Vec3 motion,Vec3 inherited,double speed,double gravity,double drag,int maxTicks) {
        return solve(start,target,motion,inherited,speed,gravity,drag,Double.POSITIVE_INFINITY,maxTicks);
    }
    public static Aim solve(Vec3 start,Vec3 target,Vec3 motion,Vec3 inherited,double speed,double gravity,double drag,double maxFallSpeed,int maxTicks) {
        // Terminal fall speed depends on launch elevation; solve against the actual forward integration.
        if(Double.isFinite(maxFallSpeed)) return terminalAim(start,target,motion,speed,gravity,drag,maxFallSpeed,maxTicks);
        return solveArc(start,target,motion,inherited,speed,gravity,drag,maxTicks,false);
    }
    public static Aim solveHighArc(Vec3 start,Vec3 target,Vec3 motion,double speed,double gravity,double drag,int maxTicks) {
        return solveArc(start,target,motion,Vec3.ZERO,speed,gravity,drag,maxTicks,true);
    }
    private static Aim solveArc(Vec3 start,Vec3 target,Vec3 motion,Vec3 inherited,double speed,double gravity,double drag,int maxTicks,boolean highArc) {
        if (speed<=0 || motion.lengthSqr()>4) return null;
        var travel=travel(gravity,drag,maxTicks);
        double previous=.05, before=required(start,target,motion,inherited,travel,previous).length()-speed;
        for (double time=.25;time<=maxTicks;time+=.25) {
            double after=required(start,target,motion,inherited,travel,time).length()-speed;
            if (highArc ? before<=0 && after>0 : before>0 && after<=0) {
                double low=previous,high=time;
                for (int i=0;i<30;i++) {
                    double middle=(low+high)/2;
                    boolean positive=required(start,target,motion,inherited,travel,middle).length()>speed;
                    if (positive!=highArc) low=middle; else high=middle;
                }
                double hitTime=(low+high)/2;
                Vec3 direction=required(start,target,motion,inherited,travel,hitTime).normalize();
                return new Aim(direction,start.add(direction.scale(target.add(motion.scale(hitTime)).distanceTo(start))),hitTime);
            }
            previous=time; before=after;
        }
        return null;
    }
    private static Aim terminalAim(Vec3 start,Vec3 target,Vec3 motion,double speed,double gravity,double drag,double maxFall,int maxTicks) {
        if(speed<=0 || motion.lengthSqr()>4) return null;
        // Each prospective flight time fixes the horizontal launch speed and target lead.
        var table=travel(gravity,drag,maxTicks);
        double previous=0; double[] before={Double.NaN,Double.NaN};
        for(double time=.25;time<=maxTicks;time+=.25) {
            if(terminalDirection(start,target,motion,speed,time,table,1)==null) { previous=time; continue; }
            if(!Double.isFinite(before[0])) {
                double low=previous,high=time;
                for(int i=0;i<30;i++) {
                    double middle=(low+high)/2;
                    if(terminalDirection(start,target,motion,speed,middle,table,1)==null) low=middle; else high=middle;
                }
                previous=high;
                for(int branch=0;branch<2;branch++) before[branch]=verticalError(start,target,motion,
                    terminalDirection(start,target,motion,speed,previous,table,branch==0?1:-1),speed,gravity,drag,maxFall,previous,table);
            }
            Aim earliest=null;
            for(int branch=0;branch<2;branch++) {
                int sign=branch==0?1:-1;
                var direction=terminalDirection(start,target,motion,speed,time,table,sign);
                double error=verticalError(start,target,motion,direction,speed,gravity,drag,maxFall,time,table);
                if(Math.signum(before[branch])!=Math.signum(error)) {
                    double low=previous,high=time;
                    for(int i=0;i<28;i++) {
                        double middle=(low+high)/2;
                        var candidate=terminalDirection(start,target,motion,speed,middle,table,sign);
                        double residual=verticalError(start,target,motion,candidate,speed,gravity,drag,maxFall,middle,table);
                        if(Math.signum(residual)==Math.signum(before[branch])) low=middle; else high=middle;
                    }
                    double ticks=(low+high)/2;
                    var aim=terminalDirection(start,target,motion,speed,ticks,table,sign);
                    var solution=new Aim(aim,start.add(aim.scale(target.add(motion.scale(ticks)).distanceTo(start))),ticks);
                    if(earliest==null || solution.ticks()<earliest.ticks()) earliest=solution;
                }
                before[branch]=error;
            }
            if(earliest!=null) return earliest;
            previous=time;
        }
        return null;
    }
    private static Vec3 terminalDirection(Vec3 start,Vec3 target,Vec3 motion,double speed,double time,TravelTable table,int sign) {
        Vec3 delta=target.add(motion.scale(time)).subtract(start);
        double factor=table.at(time).factor(),x=delta.x/factor,z=delta.z/factor;
        double verticalSquared=speed*speed-x*x-z*z;
        if(verticalSquared<0) return null;
        return new Vec3(x,sign*Math.sqrt(verticalSquared),z).scale(1/speed);
    }
    private static double verticalError(Vec3 start,Vec3 target,Vec3 motion,Vec3 direction,double speed,double gravity,double drag,double maxFall,double time,TravelTable table) {
        double velocity=direction.y*speed; int whole=(int)time;
        int clamp=whole+1;
        if(whole>=1 && velocity*drag-gravity < -maxFall) clamp=1;
        else if(whole>=1 && velocity*table.scales()[whole]-gravity*table.factors()[whole] < -maxFall) {
            int low=1,high=whole;
            while(low<high) { int middle=(low+high)/2;
                if(velocity*table.scales()[middle]-gravity*table.factors()[middle] < -maxFall) high=middle; else low=middle+1;
            }
            clamp=low;
        }
        double fraction=time-whole,position;
        if(clamp>whole) position=start.y+velocity*table.factors()[whole]-table.drops()[whole]
            +(velocity*table.scales()[whole]-gravity*table.factors()[whole])*fraction;
        else {
            position=start.y+velocity*table.factors()[clamp]-table.drops()[clamp];
            int remaining=whole-clamp;
            if(gravity>=maxFall*(1-drag)) position-=maxFall*(remaining+fraction);
            else position+=-maxFall*table.factors()[remaining]-table.drops()[remaining]
                +(-maxFall*table.scales()[remaining]-gravity*table.factors()[remaining])*fraction;
        }
        return position-(target.y+motion.y*time);
    }
    private static Vec3 required(Vec3 start,Vec3 target,Vec3 motion,Vec3 inherited,TravelTable table,double ticks) {
        Travel travel=table.at(ticks);
        return target.add(motion.scale(ticks)).subtract(start).add(0,travel.drop(),0).scale(1/travel.factor()).subtract(inherited);
    }
    private static TravelTable travel(double gravity,double drag,int maxTicks) {
        double[] factors=new double[maxTicks+2],drops=new double[maxTicks+2],scales=new double[maxTicks+2];
        scales[0]=1;
        double scale=1,vertical=0;
        for(int i=0;i<=maxTicks;i++) {
            factors[i+1]=factors[i]+scale; drops[i+1]=drops[i]-vertical;
            scale*=drag; vertical=vertical*drag-gravity;
            scales[i+1]=scale;
        }
        return new TravelTable(factors,drops,scales);
    }
}
