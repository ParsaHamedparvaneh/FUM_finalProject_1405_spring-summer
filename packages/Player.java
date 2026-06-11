package packages;

import java.util.*;

public class Player
{
    private int id;
    private String name;
    private ResourceCard resources;
    private List<Company> companies;
    private List<Partnership> partnerships;
    private String role;
    private boolean hasPlacedInitial;
    
    public Player(int id, String name)
    {
        this.id = id;
        this.name = name;
        this.resources = new ResourceCard();
        this.companies = new ArrayList<Company>();
        this.partnerships = new ArrayList<Partnership>();
        this.role = null;
        this.hasPlacedInitial = false;
    }
    
    public int getId()                         { return id; }
    public String getName()                    { return name; }
    public ResourceCard getResources()         { return resources; }
    public List<Company> getCompanies()        { return companies; }
    public List<Partnership> getPartnerships() { return partnerships; }
    public String getRole()                    { return role; }
    
    public void setRole(String role) { this.role = role; }
    
    public boolean hasPlacedInitial()            { return hasPlacedInitial; }
    public void setPlacedInitial(boolean placed) { hasPlacedInitial = placed; }
    
    public int calculateScore()
    {
        int score = 0;
        for (Company company : companies)
            score += company.getScoreValue();
        if (role != null)
            score += Constants.ROLE_PENALTY;
        return score;
    }
    
    public int getLongestPartnershipChain(Map<Partnership, List<Partnership>> adjacency)
    {
        Set<Partnership> visited = new HashSet<>();
        int maxLength = 0;
        
        for (Partnership p : partnerships)
        {
            if (!visited.contains(p))
            {
                int length = bfsLength(p, adjacency, visited);
                maxLength = Math.max(maxLength, length);
            }
        }
        return maxLength;
    }
    
    private int bfsLength(Partnership start, Map<Partnership, List<Partnership>> adjacency, Set<Partnership> visited)
    {
        Queue<Partnership> queue = new LinkedList<Partnership>();
        Map<Partnership, Integer> distance = new HashMap<Partnership, Integer>();
        queue.add(start);
        distance.put(start, 1);
        
        int maxDist = 1;
        while (!queue.isEmpty())
        {
            Partnership current = queue.poll();
            visited.add(current);
            int dist = distance.get(current);
            maxDist = Math.max(maxDist, dist);
            
            for (Partnership neighbor : adjacency.getOrDefault(current, new ArrayList<>()))
            {
                if (!distance.containsKey(neighbor))
                {
                    distance.put(neighbor, dist + 1);
                    queue.add(neighbor);
                }
            }
        }
        return maxDist;
    }
    
    public int getMaxCardsBeforeTax()
    {
        return (role != null && role.equals("VC-Funded")) ? Constants.VC_MAX_CARDS : Constants.NORMAL_MAX_CARDS;
    }
    
    public void payTax()
    {
        int totalCards = resources.getTotalCards();
        if (totalCards > getMaxCardsBeforeTax())
        {
            int toRemove = totalCards / Constants.TAX_HALF_DIVISOR;
            List<Map.Entry<String, Integer>> sorted = new ArrayList<>(resources.getAllResources().entrySet());
            sorted.sort((a, b) -> b.getValue() - a.getValue());
            
            for (Map.Entry<String, Integer> entry : sorted)
            {
                if (toRemove <= 0) break;
                int removeAmount = Math.min(entry.getValue(), toRemove);
                resources.removeResource(entry.getKey(), removeAmount);
                toRemove -= removeAmount;
            }
        }
    }
}