package ApolloRescue.module.complex.firebrigade;

import ApolloRescue.module.algorithm.clustering.ApolloFireZone;
import ApolloRescue.module.algorithm.clustering.Cluster;

public interface ITargetSelector<T> {

    /**
     *
     * @return select target
     */
    public ExtinguishTarget getTarget(ApolloFireZone targetCluster);
}
