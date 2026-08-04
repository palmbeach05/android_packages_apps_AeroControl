package com.aero.control.helpers;

import android.content.Context;
import com.aero.control.R;
import java.util.HashMap;

/* JADX INFO: loaded from: classes.dex */
public class HelpTextHolder {
    private static HelpTextHolder mHelpTextHolder;
    private Context mContext;
    private HashMap<String, String> mDataVault = new HashMap<>();

    private HelpTextHolder(Context context) {
        this.mContext = context;
        loadData();
    }

    public static synchronized HelpTextHolder instance(Context context) {
        if (mHelpTextHolder == null) {
            mHelpTextHolder = new HelpTextHolder(context);
        }
        return mHelpTextHolder;
    }

    private void putInMap(String key, int value) {
        if (this.mDataVault.containsKey(key)) {
            throw new RuntimeException("This key " + key + " does already exits in our map. Did you choose the right one?");
        }
        this.mDataVault.put(key, this.mContext.getResources().getString(value));
    }

    private void loadData() {
        putInMap("max_frequency", R.string.help_text_max_freq_cpu);
        putInMap("min_frequency", R.string.help_text_min_freq_cpu);
        putInMap("big_max_frequency", R.string.help_text_max_freq_cpu_big);
        putInMap("big_min_frequency", R.string.help_text_min_freq_cpu_big);
        putInMap("hotplug_control", R.string.help_text_hotplug_control);
        putInMap("voltage_values", R.string.help_text_voltage_values);
        putInMap("set_governor", R.string.help_text_set_governor);
        putInMap("cpu_commands", R.string.help_text_live_oc_uc);
        putInMap("gpu_max_freq", R.string.help_text_gpu_max_freq);
        putInMap("rgbValues", R.string.help_text_rgb_values);
        putInMap("set_gpu_governor", R.string.help_text_set_gpu_governor);
        putInMap("gpu_gov_settings", R.string.help_text_gpu_gov_settings);
        putInMap("read_ahead", R.string.help_text_read_ahead);
        putInMap("fsync", R.string.help_text_fsync);
        putInMap("entropy_settings", R.string.help_text_entropy_settings);
        putInMap("fstrim_toggle", R.string.help_text_fstrim_toggle);
        putInMap("dalvik_settings", R.string.help_text_dalvik_settings);
        putInMap("io_scheduler_list", R.string.help_text_io_scheduler_list);
        putInMap("ksm", R.string.help_text_ksm);
        putInMap("fifo_batch", R.string.help_text_fifo_batch);
        putInMap("front_merges", R.string.help_text_front_merges);
        putInMap("read_expire", R.string.help_text_read_expire);
        putInMap("write_expire", R.string.help_text_write_expire);
        putInMap("writes_starved", R.string.help_text_writes_starved);
        putInMap("block_dump", R.string.help_text_block_dump);
        putInMap("dirty_background_bytes", R.string.help_text_dirty_background_bytes);
        putInMap("dirty_background_ratio", R.string.help_text_dirty_background_ratio);
        putInMap("dirty_bytes", R.string.help_text_dirty_bytes);
        putInMap("dirty_expire_centisecs", R.string.help_text_dirty_expire_centisecs);
        putInMap("dirty_ratio", R.string.help_text_dirty_ratio);
        putInMap("dirty_writeback_centisecs", R.string.help_text_dirty_writeback_centisecs);
        putInMap("drop_caches", R.string.help_text_drop_caches);
        putInMap("extfrag_threshold", R.string.help_text_extfrag_threshold);
        putInMap("extra_free_kbytes", R.string.help_text_extra_free_kbytes);
        putInMap("highmem_is_dirtyable", R.string.help_text_highmem_is_dirtyable);
        putInMap("laptop_mode", R.string.help_text_laptop_mode);
        putInMap("legacy_va_layout", R.string.help_text_legacy_va_layout);
        putInMap("lowmem_reserve_ratio", R.string.help_text_lowmem_reserve_ratio);
        putInMap("max_map_count", R.string.help_text_max_map_count);
        putInMap("min_free_kbytes", R.string.help_text_min_free_kbytes);
        putInMap("oom_dump_tasks", R.string.help_text_oom_dump_tasks);
        putInMap("oom_kill_allocating_task", R.string.help_text_oom_kill_allocating_task);
        putInMap("overcommit_memory", R.string.help_text_overcommit_memory);
        putInMap("overcommit_ratio", R.string.help_text_overcommit_ratio);
        putInMap("panic_on_oom", R.string.help_text_panic_on_oom);
        putInMap("percpu_pagelist_fraction", R.string.help_text_percpu_pagelist_fraction);
        putInMap("stat_interval", R.string.help_text_stat_interval);
        putInMap("swappiness", R.string.help_text_swappiness);
        putInMap("vfs_cache_pressure", R.string.help_text_vfs_cache_pressure);
        putInMap("vtg_level", R.string.help_text_vtg_level);
        putInMap("tcp_congestion", R.string.help_text_tcp_congestion);
        putInMap("temp_threshold", R.string.help_text_temp_threshold);
        putInMap("volume_boost", R.string.help_text_volume_boost);
        putInMap("amp", R.string.help_text_amp);
        putInMap("all_cpus_threshold", R.string.help_text_all_cpus_threshold);
        putInMap("battery_saver", R.string.help_text_battery_saver);
        putInMap("debug", R.string.help_text_debug);
        putInMap("hotplug_sampling", R.string.help_text_hotplug_sampling);
        putInMap("low_latency", R.string.help_text_low_latency);
        putInMap("min_online_time", R.string.help_text_min_online_time);
        putInMap("single_core_threshold", R.string.help_text_single_core_threshold);
        putInMap("up_frequency", R.string.help_text_up_frequency);
        putInMap("above_hispeed_delay", R.string.help_text_above_hispeed_delay);
        putInMap("align_windows", R.string.help_text_align_windows);
        putInMap("boostpulse_duration", R.string.help_text_boostpulse_duration);
        putInMap("go_hispeed_load", R.string.help_text_go_hispeed_load);
        putInMap("hispeed_freq", R.string.help_text_hispeed_freq);
        putInMap("input_boost_freq", R.string.help_text_input_boost_freq);
        putInMap("io_is_busy", R.string.help_text_io_is_busy);
        putInMap("max_freq_hysteresis", R.string.help_text_max_freq_hysteresis);
        putInMap("min_sample_time", R.string.help_text_min_sample_time);
        putInMap("target_loads", R.string.help_text_target_loads);
        putInMap("timer_rate", R.string.help_text_timer_rate);
        putInMap("timer_slack", R.string.help_text_timer_slack);
        putInMap("sampling_rate", R.string.help_text_sampling_rate);
        putInMap("up_threshold", R.string.help_text_up_threshold);
        putInMap("ignore_nice_load", R.string.help_text_ignore_nice_load);
        putInMap("sampling_down_factor", R.string.help_text_sampling_down_factor);
        putInMap("powersave_bias", R.string.help_text_powersave_bias);
        putInMap("led_charging", R.string.pref_charging_led_summary);
        putInMap("multi_touch", R.string.pref_multitouch_summary);
        putInMap("button_brightness", R.string.help_text_button_brightness);
    }

    public String getText(String key) {
        return this.mDataVault.containsKey(key) ? this.mDataVault.get(key) : this.mContext.getResources().getString(R.string.help_text_no_data_found);
    }
}
