package com.aero.control.helpers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Detects the cpufreq clusters exposed by the kernel (e.g. big.LITTLE
 * layouts) so callers can build one set of controls per cluster instead of
 * assuming a fixed CPU 0-3 / CPU 4+ split.
 */
public class CpuClusterHelper {
    private static final String RELATED_CPUS = "related_cpus";
    private static final String AFFECTED_CPUS = "affected_cpus";

    private List<Cluster> mClusters;

    public List<Cluster> getClusters() {
        if (mClusters == null) {
            mClusters = detectClusters();
        }
        return mClusters;
    }

    private List<Cluster> detectClusters() {
        int cpuCount = Runtime.getRuntime().availableProcessors();
        List<List<Integer>> seenMemberLists = new ArrayList<>();
        List<Cluster> clusters = new ArrayList<>();
        for (int cpu = 0; cpu < cpuCount; cpu++) {
            List<Integer> members = readTopology(cpu);
            if (members == null || members.isEmpty()) {
                members = new ArrayList<>();
                members.add(cpu);
            }
            if (!seenMemberLists.contains(members)) {
                seenMemberLists.add(members);
                clusters.add(new Cluster(members));
            }
        }
        Collections.sort(clusters, new Comparator<Cluster>() {
            @Override
            public int compare(Cluster a, Cluster b) {
                return a.getRepresentativeCpu() - b.getRepresentativeCpu();
            }
        });
        return clusters;
    }

    private List<Integer> readTopology(int cpu) {
        String basePath = FilePath.CPU_BASE_PATH + cpu + "/cpufreq/";
        List<Integer> members = parseCpuList(readFirstLine(basePath + RELATED_CPUS));
        if (members == null) {
            members = parseCpuList(readFirstLine(basePath + AFFECTED_CPUS));
        }
        return members;
    }

    private String readFirstLine(String path) {
        File file = new File(path);
        if (!file.exists()) {
            return null;
        }
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            return reader.readLine();
        } catch (IOException e) {
            return null;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private List<Integer> parseCpuList(String raw) {
        if (raw == null || raw.trim().length() == 0) {
            return null;
        }
        String[] tokens = raw.trim().split("\\s+");
        List<Integer> result = new ArrayList<>();
        try {
            for (String token : tokens) {
                if (token.length() == 0) {
                    continue;
                }
                if (token.indexOf('-') > 0) {
                    String[] range = token.split("-");
                    int start = Integer.parseInt(range[0].trim());
                    int end = Integer.parseInt(range[1].trim());
                    for (int cpu = start; cpu <= end; cpu++) {
                        result.add(cpu);
                    }
                } else {
                    result.add(Integer.parseInt(token.trim()));
                }
            }
        } catch (NumberFormatException e) {
            return null;
        }
        if (result.isEmpty()) {
            return null;
        }
        Collections.sort(result);
        return result;
    }

    /** A group of CPUs that share the same cpufreq policy. */
    public static final class Cluster {
        private final List<Integer> mMembers;

        private Cluster(List<Integer> members) {
            mMembers = Collections.unmodifiableList(new ArrayList<>(members));
        }

        public List<Integer> getMembers() {
            return mMembers;
        }

        public int getRepresentativeCpu() {
            return mMembers.get(0);
        }

        /** Human readable member range, e.g. "0" or "0-3". */
        public String getMemberRangeLabel() {
            int min = mMembers.get(0);
            int max = mMembers.get(mMembers.size() - 1);
            return min == max ? String.valueOf(min) : min + "-" + max;
        }
    }
}