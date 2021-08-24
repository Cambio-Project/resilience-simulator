package cambio.simulator.entities.patterns;

import com.google.gson.annotations.Expose;
import desmoj.core.simulator.Model;

/**
 * @author Lion Wagner
 */
public abstract class StrategicServiceOwnedPattern<S extends IStrategy> extends ServiceOwnedPattern implements
    IStrategyAcceptor<S> {
    @Expose
    protected S strategy;

    public StrategicServiceOwnedPattern(Model model, String name, boolean showInTrace) {
        super(model, name, showInTrace);
    }

    @Override
    public S getStrategy() {
        return strategy;
    }

    @Override
    public void setStrategy(S strategy) {
        this.strategy = strategy;
    }
}
