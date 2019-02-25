package ApolloRescue.module.universal.knd;

import java.util.Arrays;

public class HungarianAlgorithm {
    private final double[][] costMatrix;
    private final int rows;
    private final int cols;
    private final int dim;
    private final double[] labelByWorker;
    private final double[] labelByJob;
    private final int[] minSlackWorkerByJob;
    private final double[] minSlackValueByJob;
    private final int[] matchJobByWorker;
    private final int[] matchWorkerByJob;
    private final int[] parentWorkerByCommittedJob;
    private final boolean[] committedWorkers;

    public HungarianAlgorithm(double[][] var1) {
        this.dim = Math.max(var1.length, var1[0].length);
        this.rows = var1.length;
        this.cols = var1[0].length;
        this.costMatrix = new double[this.dim][this.dim];

        for(int var2 = 0; var2 < this.dim; ++var2) {
            if (var2 >= var1.length) {
                this.costMatrix[var2] = new double[this.dim];
            } else {
                if (var1[var2].length != this.cols) {
                    throw new IllegalArgumentException("Irregular cost matrix");
                }

                for(int var3 = 0; var3 < this.cols; ++var3) {
                    if (Double.isInfinite(var1[var2][var3])) {
                        throw new IllegalArgumentException("Infinite cost");
                    }

                    if (Double.isNaN(var1[var2][var3])) {
                        throw new IllegalArgumentException("NaN cost");
                    }
                }

                this.costMatrix[var2] = Arrays.copyOf(var1[var2], this.dim);
            }
        }

        this.labelByWorker = new double[this.dim];
        this.labelByJob = new double[this.dim];
        this.minSlackWorkerByJob = new int[this.dim];
        this.minSlackValueByJob = new double[this.dim];
        this.committedWorkers = new boolean[this.dim];
        this.parentWorkerByCommittedJob = new int[this.dim];
        this.matchJobByWorker = new int[this.dim];
        Arrays.fill(this.matchJobByWorker, -1);
        this.matchWorkerByJob = new int[this.dim];
        Arrays.fill(this.matchWorkerByJob, -1);
    }

    protected void computeInitialFeasibleSolution() {
        int var1;
        for(var1 = 0; var1 < this.dim; ++var1) {
            this.labelByJob[var1] = 1.0D / 0.0;
        }

        for(var1 = 0; var1 < this.dim; ++var1) {
            for(int var2 = 0; var2 < this.dim; ++var2) {
                if (this.costMatrix[var1][var2] < this.labelByJob[var2]) {
                    this.labelByJob[var2] = this.costMatrix[var1][var2];
                }
            }
        }

    }

    public int[] execute() {
        this.reduce();
        this.computeInitialFeasibleSolution();
        this.greedyMatch();

        int var1;
        for(var1 = this.fetchUnmatchedWorker(); var1 < this.dim; var1 = this.fetchUnmatchedWorker()) {
            this.initializePhase(var1);
            this.executePhase();
        }

        int[] var2 = Arrays.copyOf(this.matchJobByWorker, this.rows);

        for(var1 = 0; var1 < var2.length; ++var1) {
            if (var2[var1] >= this.cols) {
                var2[var1] = -1;
            }
        }

        return var2;
    }

    protected void executePhase() {
        label50:
        while(true) {
            int var1 = -1;
            int var2 = -1;
            double var3 = 1.0D / 0.0;

            int var5;
            for(var5 = 0; var5 < this.dim; ++var5) {
                if (this.parentWorkerByCommittedJob[var5] == -1 && this.minSlackValueByJob[var5] < var3) {
                    var3 = this.minSlackValueByJob[var5];
                    var1 = this.minSlackWorkerByJob[var5];
                    var2 = var5;
                }
            }

            if (var3 > 0.0D) {
                this.updateLabeling(var3);
            }

            this.parentWorkerByCommittedJob[var2] = var1;
            int var6;
            if (this.matchWorkerByJob[var2] != -1) {
                var5 = this.matchWorkerByJob[var2];
                this.committedWorkers[var5] = true;
                var6 = 0;

                while(true) {
                    if (var6 >= this.dim) {
                        continue label50;
                    }

                    if (this.parentWorkerByCommittedJob[var6] == -1) {
                        double var9 = this.costMatrix[var5][var6] - this.labelByWorker[var5] - this.labelByJob[var6];
                        if (this.minSlackValueByJob[var6] > var9) {
                            this.minSlackValueByJob[var6] = var9;
                            this.minSlackWorkerByJob[var6] = var5;
                        }
                    }

                    ++var6;
                }
            }

            var5 = var2;
            var6 = this.parentWorkerByCommittedJob[var2];

            while(true) {
                int var7 = this.matchJobByWorker[var6];
                this.match(var6, var5);
                var5 = var7;
                if (var7 == -1) {
                    return;
                }

                var6 = this.parentWorkerByCommittedJob[var7];
            }
        }
    }

    protected int fetchUnmatchedWorker() {
        int var1;
        for(var1 = 0; var1 < this.dim && this.matchJobByWorker[var1] != -1; ++var1) {
            ;
        }

        return var1;
    }

    protected void greedyMatch() {
        for(int var1 = 0; var1 < this.dim; ++var1) {
            for(int var2 = 0; var2 < this.dim; ++var2) {
                if (this.matchJobByWorker[var1] == -1 && this.matchWorkerByJob[var2] == -1 && this.costMatrix[var1][var2] - this.labelByWorker[var1] - this.labelByJob[var2] == 0.0D) {
                    this.match(var1, var2);
                }
            }
        }

    }

    protected void initializePhase(int var1) {
        Arrays.fill(this.committedWorkers, false);
        Arrays.fill(this.parentWorkerByCommittedJob, -1);
        this.committedWorkers[var1] = true;

        for(int var2 = 0; var2 < this.dim; ++var2) {
            this.minSlackValueByJob[var2] = this.costMatrix[var1][var2] - this.labelByWorker[var1] - this.labelByJob[var2];
            this.minSlackWorkerByJob[var2] = var1;
        }

    }

    protected void match(int var1, int var2) {
        this.matchJobByWorker[var1] = var2;
        this.matchWorkerByJob[var2] = var1;
    }

    protected void reduce() {
        for(int var1 = 0; var1 < this.dim; ++var1) {
            double var2 = 1.0D / 0.0;

            int var4;
            for(var4 = 0; var4 < this.dim; ++var4) {
                if (this.costMatrix[var1][var4] < var2) {
                    var2 = this.costMatrix[var1][var4];
                }
            }

            for(var4 = 0; var4 < this.dim; ++var4) {
                this.costMatrix[var1][var4] -= var2;
            }
        }

        double[] var5 = new double[this.dim];

        int var6;
        for(var6 = 0; var6 < this.dim; ++var6) {
            var5[var6] = 1.0D / 0.0;
        }

        int var3;
        for(var6 = 0; var6 < this.dim; ++var6) {
            for(var3 = 0; var3 < this.dim; ++var3) {
                if (this.costMatrix[var6][var3] < var5[var3]) {
                    var5[var3] = this.costMatrix[var6][var3];
                }
            }
        }

        for(var6 = 0; var6 < this.dim; ++var6) {
            for(var3 = 0; var3 < this.dim; ++var3) {
                this.costMatrix[var6][var3] -= var5[var3];
            }
        }

    }

    protected void updateLabeling(double var1) {
        int var3;
        for(var3 = 0; var3 < this.dim; ++var3) {
            if (this.committedWorkers[var3]) {
                this.labelByWorker[var3] += var1;
            }
        }

        for(var3 = 0; var3 < this.dim; ++var3) {
            if (this.parentWorkerByCommittedJob[var3] != -1) {
                this.labelByJob[var3] -= var1;
            } else {
                this.minSlackValueByJob[var3] -= var1;
            }
        }

    }
}
