package packages;

public class InsufficientResourcesException extends Exception
{
    public InsufficientResourcesException(String message)
    {
        super(message);
    }
}