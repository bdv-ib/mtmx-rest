package com.diximonline.mtmx.conv.efts;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum MtTypes {
	
	MT195("195", "pacs.002.001.05", false),
	MT103("103", "pacs.008.001.04", true),
	MT104("104", "pacs.003.001.04", true)
	;
	
	public String mtType;
	public String defaultMxType;
	public Boolean isPayment;
	
	MtTypes (String mtType, String mxType, Boolean isPayment) {
		this.mtType = mtType;
		this.defaultMxType = mxType;
		this.isPayment = isPayment;
	}
	
	public static MtTypes findByType(String type) {
		for (MtTypes mt : MtTypes.values()) {
			if (mt.mtType.compareTo(type) == 0) {
				return mt;
			}
		}
		return null;
	}
}
