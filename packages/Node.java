package packages;

import java.util.*;

public class Node
{
    private Point position;
    private Company company;
    private List<Sector> adjacentSectors;
    
    public Node(int x, int y)
    {
        this.position = new Point(x, y);
        this.adjacentSectors = new ArrayList<Sector>();
        this.company = null;
    }
    
    public Point getPosition()                   { return position; }
    public Company getCompany()                  { return company; }
    public void setCompany(Company company)      { this.company = company; }
    public boolean hasCompany()                  { return company != null; }
    public void addAdjacentSector(Sector sector) { adjacentSectors.add(sector); }
    public List<Sector> getAdjacentSectors()     { return adjacentSectors; }
    
    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof Node)
        {
            Node other = (Node) obj;
            return position.equals(other.position);
        }
        return false;
    }
    
    @Override
    public int hashCode() { return position.hashCode(); }
}