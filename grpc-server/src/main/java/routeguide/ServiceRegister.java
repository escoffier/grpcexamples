package routeguide;

public interface ServiceRegister {
    public void registerService(ServiceConfig config) throws Exception;
    public void unregisterService();
}
