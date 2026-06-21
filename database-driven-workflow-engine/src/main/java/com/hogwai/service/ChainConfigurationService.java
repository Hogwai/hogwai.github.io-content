package com.hogwai.service;

import com.hogwai.dto.ChainConfigurationRecord;

public interface ChainConfigurationService {
    ChainConfigurationRecord getChainConfigurationByName(String chainConfName);
    ChainConfigurationRecord createChainConfiguration(ChainConfigurationRecord configurationDto);
    ChainConfigurationRecord updateChainConfiguration(ChainConfigurationRecord chainConfigurationRecord);
}
