package ApolloRescue.module.complex.component;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import ApolloRescue.module.universal.ApolloWorld;
import javolution.util.FastMap;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.Property;


public class PropertyComponent implements IComponent {

    protected ApolloWorld world;
    protected Map<Property, Integer> propertyTimeMap = new FastMap<Property, Integer>();

    public PropertyComponent(ApolloWorld world) {
        this.world = world;
    }

    public void init() {
//        Collection<StandardEntity> entities = new HashSet<StandardEntity>();
//        entities.addAll(world.getBuildings());
//        entities.addAll(world.getRoads());
//        entities.addAll(world.getHumans());

        propertyTimeMap.clear();

//        for (StandardEntity entity : entities) {
//            for (Property property : entity.getProperties()) {
//                propertyTimeMap.put(property, 0);
//            }
//        }
    }

    public void update() {

    }

    public int getEntityLastUpdateTime(StandardEntity entity) {
        int maxTime = Integer.MIN_VALUE;
        for (Property property : entity.getProperties()) {
            Integer value = getPropertyTime(property);
            if (value > maxTime) {
                maxTime = value;
            }
        }

        return maxTime;
    }

    public Integer getPropertyTime(Property property) {
        Integer integer = propertyTimeMap.get(property);
        if (integer == null) {
            return 0;
        }
        return integer;
    }

    public void setPropertyTime(Property property, Integer time) {
        propertyTimeMap.put(property, time);
    }

    public void addEntityProperty(StandardEntity entity, int time) {
        for (Property property : entity.getProperties()) {
            propertyTimeMap.put(property, time);
        }

    }
}