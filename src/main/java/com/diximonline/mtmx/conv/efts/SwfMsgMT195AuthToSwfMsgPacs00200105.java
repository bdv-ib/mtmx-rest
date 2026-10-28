package com.diximonline.mtmx.conv.efts;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.diximonline.conv.IConvToModel;
import com.diximonline.conv.MxJaxbConverter;
import com.diximonline.core.CoreException;
import com.diximonline.core.integration.msg.ENFormat;
import com.diximonline.core.utils.FastDateFmt;
import com.diximonline.core.utils.SwiftUtils;
import com.diximonline.core.utils.Utils;
import com.diximonline.iso20022.bh.efts.pacs00200105.BranchAndFinancialInstitutionIdentification5;
import com.diximonline.iso20022.bh.efts.pacs00200105.Document;
import com.diximonline.iso20022.bh.efts.pacs00200105.FIToFIPaymentStatusReportV05CMA;
import com.diximonline.iso20022.bh.efts.pacs00200105.FinancialInstitutionIdentification8;
import com.diximonline.iso20022.bh.efts.pacs00200105.GroupHeader53CMA;
import com.diximonline.iso20022.bh.efts.pacs00200105.OriginalGroupHeaderCMA1;
import com.diximonline.iso20022.bh.efts.pacs00200105.OriginalTransactionReference16;
import com.diximonline.iso20022.bh.efts.pacs00200105.PaymentTransaction33CMA;
import com.diximonline.iso20022.bh.efts.pacs00200105.StatusReason6ChoiceCMA1;
import com.diximonline.iso20022.bh.efts.pacs00200105.StatusReasonInformation9CMA1;
import com.diximonline.iso20022.model.AppHdr;
import com.diximonline.payments.model.status.PaymentStatusModel;
import com.diximonline.payments.model.status.StsRsnInf;
import com.diximonline.payments.model.status.TxInfAndSts;
import com.prowidesoftware.swift.io.parser.SwiftParser;
import com.prowidesoftware.swift.model.SwiftBlock4;
import com.prowidesoftware.swift.model.Tag;

import lombok.val;
import lombok.extern.slf4j.Slf4j;
import se.highex.ebe.adaptor.connector.pg.PgParamsMtMsg;

@Slf4j
public class SwfMsgMT195AuthToSwfMsgPacs00200105 extends MxJaxbConverter<Document> implements IConvToModel<PgParamsMtMsg, PgParamsMtMsg> {
	
	@Override
	public PgParamsMtMsg toModel(PgParamsMtMsg swfMsg, Map<String, String> headers) throws CoreException {
		
		try {
			
			SwiftBlock4 block4 = SwiftParser.parseBlock4("{4:\r\n" + swfMsg.getBlock4() + "\r\n-}");
			
			PaymentStatusModel pm = new PaymentStatusModel();
			StsRsnInf grpStsRsn = new StsRsnInf();
			
			String f20Value = block4.getTagByName("20").getValue();
			
			pm.setMsgId(f20Value);
			pm.setCreationDate(new Date());

			String f21Value = block4.getTagByName("21").getValue();
			
			pm.setOrgnlMsgId(f21Value);
			
			String f11SValue = block4.getTagByName("11S").getValue();
			List<String> f11SValues = SwiftUtils.splitString(f11SValue);
			
			//INFO: format yyMMdd
			pm.setOrgnlСreationDate(f11SValues.get(1));
			
			String orgnlMtTypeStr = f11SValues.get(0);
			MtTypes orgnlMtType = MtTypes.findByType(orgnlMtTypeStr);
			String orgnlMxType = orgnlMtType.defaultMxType;
			
			pm.setOrgnlMessageType(orgnlMxType);
			
			String f79Value = block4.getTagByName("79").getValue();
			List<String> f79Values = SwiftUtils.splitString(f79Value);
			
			pm.setInstructingAgent(f79Values.get(0));
			
			String status = block4.getTagByName("75").getValue();
			
			pm.setGroupStatus(status);
			pm.setGroupStatusReasonInfo(grpStsRsn);
			
			grpStsRsn.setErrorCode(status);
			
			Tag f77ATag = block4.getTagByName("77A");
			
			if (f77ATag != null && f77ATag.getValue() != null) {
				List<String> f77AValues = SwiftUtils.splitString(f77ATag.getValue());
				grpStsRsn.setErrorDescription(f77AValues);
			}
			
			TxInfAndSts txStatus = new TxInfAndSts();
			
			txStatus.setInstructingAgent(f79Values.get(0));
			
			Date valueDate = FastDateFmt.YYMMDD.parse(f79Values.get(1));
			
			txStatus.setValueDate(valueDate);
			
			pm.setInstructionsStatuses(new ArrayList<TxInfAndSts>());
			pm.getInstructionsStatuses().add(txStatus);
			
			//INFO: convert model to MX
			AppHdr appHdr = AppHdr.builder().bizMsgIdr(pm.getMsgId())
					.frBICFI(SwiftUtils.getBICByLT(swfMsg.getMsgSender()))
					.toBICFI(SwiftUtils.getBICByLT(swfMsg.getMsgReceiver()))
					.creDt(pm.getCreationDate())
					.msgDefIdr(getMsgType())
					.build();

			val doc = new Document();
			
			doc.setFIToFIPmtStsRpt(new FIToFIPaymentStatusReportV05CMA());
			doc.getFIToFIPmtStsRpt().setGrpHdr(new GroupHeader53CMA());
			
			doc.getFIToFIPmtStsRpt().getGrpHdr().setMsgId(pm.getMsgId());
			doc.getFIToFIPmtStsRpt().getGrpHdr().setCreDtTm(Utils.toXMLGregorianCalendar(pm.getCreationDate(), false, Utils.THROWS));
			
			doc.getFIToFIPmtStsRpt().setOrgnlGrpInfAndSts(new OriginalGroupHeaderCMA1());
			doc.getFIToFIPmtStsRpt().getOrgnlGrpInfAndSts().setOrgnlMsgId(pm.getOrgnlMsgId());
			doc.getFIToFIPmtStsRpt().getOrgnlGrpInfAndSts().setOrgnlMsgNmId(pm.getOrgnlMessageType());
			
			Date orgnlCrDtTm = FastDateFmt.YYMMDD.parse(pm.getOrgnlСreationDate());
			
			doc.getFIToFIPmtStsRpt().getOrgnlGrpInfAndSts().setOrgnlCreDtTm(Utils.toXMLGregorianCalendar(orgnlCrDtTm, false, Utils.THROWS));
			doc.getFIToFIPmtStsRpt().getOrgnlGrpInfAndSts().setStsRsnInf(new StatusReasonInformation9CMA1());
			doc.getFIToFIPmtStsRpt().getOrgnlGrpInfAndSts().getStsRsnInf().setRsn(new StatusReason6ChoiceCMA1());
			doc.getFIToFIPmtStsRpt().getOrgnlGrpInfAndSts().getStsRsnInf().getRsn().setPrtry(pm.getGroupStatus());
			
			if (pm.getGroupStatusReasonInfo().getErrorDescription() != null && pm.getGroupStatusReasonInfo().getErrorDescription().size() > 0) {
				doc.getFIToFIPmtStsRpt().getOrgnlGrpInfAndSts().getStsRsnInf().getAddtlInf().addAll(pm.getGroupStatusReasonInfo().getErrorDescription());
			}

			PaymentTransaction33CMA pmtSts = new PaymentTransaction33CMA();
			doc.getFIToFIPmtStsRpt().getTxInfAndSts().add(pmtSts);
			
			pmtSts.setInstgAgt(new BranchAndFinancialInstitutionIdentification5());
			pmtSts.getInstgAgt().setFinInstnId(new FinancialInstitutionIdentification8());
			pmtSts.getInstgAgt().getFinInstnId().setBICFI(txStatus.getInstructingAgent());
			
			pmtSts.setOrgnlTxRef(new OriginalTransactionReference16());
			pmtSts.getOrgnlTxRef().setIntrBkSttlmDt(Utils.toXMLGregorianCalendar(txStatus.getValueDate(), false, Utils.THROWS));
			
			String block4Str = toMX(appHdr, doc);
			
			swfMsg.setFormat(ENFormat.MX.name());
			swfMsg.setBlock4(block4Str);
			swfMsg.setMsgType(getMsgType());
			
			return swfMsg;
		} catch (Exception e) {
			log.error("Error converting MT195/AUTH wrapped into SwiftMsgFields into pacs.002.001.05 MX message", e);
			throw new CoreException(CoreException.PARSE_ERROR, "");
		}
	}

	@Override
	public String getMsgType() {
		return "pacs.002.001.05";
	}

	@Override
	public Class<?>[] getClasses() {
		return new Class<?>[] {Document.class};
	}
}
