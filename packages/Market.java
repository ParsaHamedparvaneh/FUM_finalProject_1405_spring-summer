package packages;

import java.util.*;

public class Market {
    private Map<String, Integer> prices;
    private Map<String, Integer> turnsSinceLastPurchase;
    private int currentTurn;
    
    public Market()
    {
        prices = new HashMap<String, Integer>();
        turnsSinceLastPurchase = new HashMap<String, Integer>();
        for (String resource : Constants.RESOURCES)
        {
            prices.put(resource, Constants.BASE_PRICE);
            turnsSinceLastPurchase.put(resource, 0);
        }
        currentTurn = 0;
    }
    
    public int getPrice(String resource)       { return prices.getOrDefault(resource, Constants.BASE_PRICE); }
    public Map<String, Integer> getAllPrices() { return new HashMap<>(prices); }
    
    public void newTurn()
    {
        currentTurn++;
        for (String resource : Constants.RESOURCES)
        {
            int turns = turnsSinceLastPurchase.get(resource);
            if (turns >= Constants.DECAY_THRESHOLD)
            {
                int newPrice = Math.max(Constants.MIN_PRICE, prices.get(resource) - Constants.PRICE_INCREASE);
                prices.put(resource, newPrice);
            }
            turnsSinceLastPurchase.put(resource, turns + 1);
        }
    }
    
    public boolean buyResource(Player player, String resource)
    {
        int price = getPrice(resource);
        if (player.getResources().getCount(Constants.RESOURCE_CAPITAL) >= price)
        {
            player.getResources().removeResource(Constants.RESOURCE_CAPITAL, price);
            player.getResources().addResource(resource, 1);
            
            int newPrice = Math.min(Constants.MAX_PRICE, price + Constants.PRICE_INCREASE);
            prices.put(resource, newPrice);
            turnsSinceLastPurchase.put(resource, 0);
            return true;
        }
        return false;
    }
}