package com.milesight.beaveriot.integration.msc.filter;

import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Filter for EM500-SMTC device model
 * Only keeps essential properties, events and services for performance optimization
 */
@Component
public class EM500SMTCThingSpecFilter implements ThingSpecFilterProvider {

    @Override
    public String getModelPattern() {
        return "EM500-SMTC*";
    }

    @Override
    public FilterMode getMode() {
        return FilterMode.WHITELIST;
    }

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public Set<String> getIds() {
        return Set.of(
                "device_status",
                "ipso_version",
                "sn",
                "hardware_version",
                "firmware_version",
                "lorawan_class",
                "battery",
                "conductivity",
                "temperature",
                "soil_moisture",
                "temperature_mutation_alarm",
                "temperature_mutation_alarm.temperature",
                "temperature_mutation_alarm.temperature_mutation_value",
                "temperature_mutation_alarm.alarm_type",
                "historical_data_retrieve",
                "historical_data_retrieve.timestamp",
                "historical_data_retrieve.electrical_conductivity",
                "historical_data_retrieve.temperature",
                "historical_data_retrieve.soil_moisture",
                "reporting_interval",
                "time_zone",
                "reset_collection_settings",
                "reset_collection_settings.times",
                "reset_collection_settings.interval",
                "sensor_temperature_enable",
                "sensor_temperature_enable.channel",
                "sensor_temperature_enable.enable",
                "sensor_humidity_enable",
                "sensor_humidity_enable.channel",
                "sensor_humidity_enable.enable",
                "sensor_electrical_conductivity_enable",
                "sensor_electrical_conductivity_enable.channel",
                "sensor_electrical_conductivity_enable.enable",
                "collection_interval",
                "temperature_calibration_settings",
                "temperature_calibration_settings.id",
                "temperature_calibration_settings.enable",
                "temperature_calibration_settings.value",
                "humidity_calibration_settings",
                "humidity_calibration_settings.id",
                "humidity_calibration_settings.enable",
                "humidity_calibration_settings.value",
                "electrical_conductivity_calibration_settings",
                "electrical_conductivity_calibration_settings.id",
                "electrical_conductivity_calibration_settings.enable",
                "electrical_conductivity_calibration_settings.value",
                "data_storage_enable",
                "retransmission_enable",
                "retransmission_interval_settings",
                "retransmission_interval_settings.type",
                "retransmission_interval_settings.interval",
                "retrieval_interval",
                "retrieval_interval.type",
                "retrieval_interval.interval",
                "clear_historical_data",
                "retrieve_historical_data_by_time",
                "retrieve_historical_data_by_time.time",
                "retrieve_historical_data_by_time_range",
                "retrieve_historical_data_by_time_range.start_time",
                "retrieve_historical_data_by_time_range.end_time",
                "stop_historical_data_retrival",
                "synchronize_time",
                "query_device_status",
                "reboot"
        );
    }
}
