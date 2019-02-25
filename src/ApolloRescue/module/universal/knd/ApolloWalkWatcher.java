package ApolloRescue.module.universal.knd;

import adf.agent.action.common.ActionMove;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.component.module.AbstractModule;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.List;

public class ApolloWalkWatcher extends AbstractModule{
    AgentInfo ai = null;
    ApolloRandomDirectSelector randomDirectSelector = null;
    private int ignoreUntil = 0;
    public ArrayList<Step> recentSteps = new ArrayList();

    public ApolloWalkWatcher(AgentInfo var1, WorldInfo var2, ScenarioInfo var3, ModuleManager var4, DevelopData var5) {
        super(var1, var2, var3, var4, var5);
        this.ai = var1;
        this.randomDirectSelector = new ApolloRandomDirectSelector(var1, var2);
        this.ignoreUntil = var3.getKernelAgentsIgnoreuntil();
    }

    public AbstractModule calc() {
        return null;
    }

    private void add(List<EntityID> var1) {
        this.recentSteps.add(new ApolloWalkWatcher.Step(this.ai.getPosition().getValue(), ((EntityID)var1.get(0)).getValue()));
        if (this.recentSteps.size() > 4) {
            this.recentSteps.remove(0);
        }

    }

    private boolean anyProblem() {
        int var1 = this.recentSteps.size();
        if (var1 < 2) {
            return false;
        } else {
            ApolloWalkWatcher.Step var2 = (ApolloWalkWatcher.Step)this.recentSteps.get(var1 - 1);
            ApolloWalkWatcher.Step var3 = (ApolloWalkWatcher.Step)this.recentSteps.get(var1 - 2);
            return var2.equals(var3);
        }
    }

    public ActionMove check(ActionMove var1) {
        if (var1 != null && var1.getPath() != null && var1.getPath().size() != 0) {
            if (this.ai.getTime() < this.ignoreUntil) {
                return var1;
            } else {
                this.randomDirectSelector.update();
                this.add(var1.getPath());
                if (this.anyProblem()) {
                    this.randomDirectSelector.generate();
                    ArrayList var2 = new ArrayList();
                    var2.add(this.ai.getPosition());
                    return new ActionMove(var2, (int)this.randomDirectSelector.generatedPoint.getX(), (int)this.randomDirectSelector.generatedPoint.getY());
                } else {
                    return var1;
                }
            }
        } else {
            return null;
        }
    }

    class Step {
        int fromID;
        int toID;
        double fromX = 0.0D;
        double fromY = 0.0D;

        public Step(int var2, int var3) {
            this.fromID = var2;
            this.toID = var3;
            this.fromX = ApolloWalkWatcher.this.ai.getX();
            this.fromY = ApolloWalkWatcher.this.ai.getY();
        }

        public boolean equals(Object var1) {
            if (var1 != null && var1 instanceof ApolloWalkWatcher.Step) {
                ApolloWalkWatcher.Step var2 = (ApolloWalkWatcher.Step)var1;
                double var3 = (var2.fromX - this.fromX) * (var2.fromX - this.fromX);
                var3 += (var2.fromY - this.fromY) * (var2.fromY - this.fromY);
                return var3 < 562500.0D;
            } else {
                return false;
            }
        }
    }
}
