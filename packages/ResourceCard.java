package packages;

import java.util.*;

public class ResourceCard
{
    private Map<String, Integer> resources;
    
    public ResourceCard()
    {
        resources = new HashMap<String, Integer>();
        for (String resource : Constants.RESOURCES)
            resources.put(resource, 0);
    }
    
    public int getCount(String resource)                 { return resources.getOrDefault(resource, 0); }
    public int getTotalCards()                           { return resources.values().stream().mapToInt(Integer::intValue).sum(); }
    public Map<String, Integer> getAllResources()        { return new HashMap<String, Integer>(resources); }
    
    public void addResource(String resource, int amount) { resources.put(resource, getCount(resource) + amount); }
    public boolean removeResource(String resource, int amount)
    {
        if (getCount(resource) >= amount)
        {
            resources.put(resource, (getCount(resource) - amount));
            return true;
        }
        return false;
    }
    
    public boolean hasEnough(Map<String, Integer> required)
    {
        for (Map.Entry<String, Integer> entry : required.entrySet())
            if (getCount(entry.getKey()) < entry.getValue())
                return false;

        return true;
    }
    public void deductResources(Map<String, Integer> cost)
    {
        for (Map.Entry<String, Integer> entry : cost.entrySet())
            removeResource(entry.getKey(), entry.getValue());
    }
}