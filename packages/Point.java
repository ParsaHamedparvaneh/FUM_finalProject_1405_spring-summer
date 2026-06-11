package packages;

public class Point
{
    public int x;
    public int y;
    
    public Point(int x, int y)
    {
        this.x = x;
        this.y = y;
    }
    
    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof Point)
        {
            Point other = (Point) obj;
            return (this.x == other.x && this.y == other.y);
        }
        return false;
    }
    
    @Override
    public int hashCode() { return 31 * x + y; }
}