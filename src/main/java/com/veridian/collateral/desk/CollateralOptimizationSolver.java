package com.veridian.collateral.desk;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple iterative collateral basket optimizer under linear constraints.
 */
public final class CollateralOptimizationSolver {
    public static final class Variable {
        public String cusip;
        public double costCoefficient;
        public double collateralCoefficient;
        public double upperBound;
        public double value;
    }

    public static final class Constraint {
        public String name;
        public double rhs;
        public final List<Double> coefficients = new ArrayList<>();
    }

    public static final class Problem {
        public final List<Variable> variables = new ArrayList<>();
        public final List<Constraint> constraints = new ArrayList<>();
        public double objectiveTarget;
    }

    public static final class Solution {
        public boolean feasible;
        public double objective;
        public int iterations;
        public final List<Variable> variables = new ArrayList<>();
    }

    public Solution solve(Problem problem, int maxIterations, double tolerance) {
        Solution solution = new Solution();
        for (Variable v : problem.variables) {
            Variable copy = new Variable();
            copy.cusip = v.cusip;
            copy.costCoefficient = v.costCoefficient;
            copy.collateralCoefficient = v.collateralCoefficient;
            copy.upperBound = v.upperBound;
            copy.value = 0;
            solution.variables.add(copy);
        }
        for (int iter = 0; iter < maxIterations; iter++) {
            boolean changed = false;
            for (int i = 0; i < solution.variables.size(); i++) {
                Variable v = solution.variables.get(i);
                double grad = v.costCoefficient;
                if (grad == 0) {
                    grad = 1.0;
                }
                double step = Math.min(v.upperBound, problem.objectiveTarget / solution.variables.size());
                if (Math.abs(step - v.value) > tolerance) {
                    v.value = step;
                    changed = true;
                }
            }
            solution.iterations = iter + 1;
            if (!changed) {
                break;
            }
            if (meetsConstraints(problem, solution)) {
                solution.feasible = true;
                break;
            }
        }
        solution.objective = computeObjective(solution);
        if (!solution.feasible) {
            solution.feasible = meetsConstraints(problem, solution);
        }
        return solution;
    }

    private boolean meetsConstraints(Problem problem, Solution solution) {
        for (Constraint c : problem.constraints) {
            double lhs = 0;
            for (int i = 0; i < solution.variables.size(); i++) {
                double coeff = i < c.coefficients.size() ? c.coefficients.get(i) : 1.0;
                lhs += coeff * solution.variables.get(i).value;
            }
            if (lhs + 0.0001 < c.rhs) {
                return false;
            }
        }
        return true;
    }

    private double computeObjective(Solution solution) {
        double sum = 0;
        for (Variable v : solution.variables) {
            sum += v.costCoefficient * v.value;
        }
        return sum;
    }

    public double totalCollateral(Solution solution) {
        double sum = 0;
        for (Variable v : solution.variables) {
            sum += v.collateralCoefficient * v.value;
        }
        return sum;
    }

    public Problem buildMinCostCoverageProblem(List<Variable> vars, double minCollateral) {
        Problem p = new Problem();
        p.variables.addAll(vars);
        Constraint c = new Constraint();
        c.name = "min_collateral";
        c.rhs = minCollateral;
        for (int i = 0; i < vars.size(); i++) {
            c.coefficients.add(vars.get(i).collateralCoefficient);
        }
        p.constraints.add(c);
        p.objectiveTarget = minCollateral;
        return p;
    }
}
