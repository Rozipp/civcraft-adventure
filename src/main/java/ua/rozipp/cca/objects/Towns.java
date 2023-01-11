package ua.rozipp.cca.objects;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Towns {

    private static Map<String, Town> towns = new HashMap();

    public static void add(Town town){
        towns.put(town.getName(), town);
    }

    public static void addAll(List<Object> newTowns){
        newTowns.forEach((t) -> add((Town) t));
    }

    public static Town get(String name){
        return towns.get(name);
    }

    public static void delete(Town town){
        towns.remove(town.getName());
    }

    public Town newTown(){
        return new Town();
    }
}
