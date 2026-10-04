package org.hypixelskyblockmods.crittersafariesp;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.Vec3;

/** Static Safari graph samples; ambiguous borders and positions far from the map have no biome. */
public final class SafariAreas {
    private record Sample(double x,double y,double z,SafariSpecies.Biome biome) {
        double distanceSquared(Vec3 point) { return Math.pow(x-point.x,2)+Math.pow(y-point.y,2)+Math.pow(z-point.z,2); }
    }
    private static final List<Sample> SAMPLES=load();
    private SafariAreas() {}
    private static List<Sample> load() {
        var samples=new ArrayList<Sample>();
        try(var input=SafariAreas.class.getResourceAsStream("/assets/crittersafariesp/safari-areas.csv")) {
            if(input==null) throw new IllegalStateException("Missing Safari area samples");
            try(var reader=new BufferedReader(new InputStreamReader(input,StandardCharsets.UTF_8))) {
                for(String line; (line=reader.readLine())!=null;) {
                    if(line.startsWith("#") || line.isBlank()) continue;
                    var parts=line.split(",");
                    samples.add(new Sample(Double.parseDouble(parts[0]),Double.parseDouble(parts[1]),Double.parseDouble(parts[2]),
                        parts[3].equals("NONE")?null:SafariSpecies.Biome.valueOf(parts[3])));
                }
            }
        } catch(java.io.IOException e) { throw new IllegalStateException("Cannot load Safari areas",e); }
        return List.copyOf(samples);
    }
    public static SafariSpecies.Biome at(Vec3 point) {
        Sample closest=null; double best=32*32;
        for(var sample:SAMPLES) {
            double distance=sample.distanceSquared(point);
            if(distance<best) { closest=sample; best=distance; }
        }
        if(closest==null || closest.biome()==null) return null;
        double border=Math.pow(Math.sqrt(best)+1,2);
        for(var sample:SAMPLES) if(sample.biome()!=closest.biome() && sample.distanceSquared(point)<=border) return null;
        return closest.biome();
    }
}
