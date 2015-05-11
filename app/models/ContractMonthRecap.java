package models;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import manager.recaps.residual.PersonResidualMonthRecap;
import models.base.BaseModel;

import org.hibernate.envers.Audited;

import dao.wrapper.IWrapperContract;
import play.data.validation.Required;

@Entity
@Table(name="contract_month_recap")
public class ContractMonthRecap extends BaseModel {
	
	@Required
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="contract_id")
	public Contract contract;
	
	@Column
	public int year;
	
	@Column
	public int month;
	
	/**************************************************************************
	 * MODULO RECAP ASSENZE
	 **************************************************************************/
	
	@Column(name="abs_fap_usate")
	public Integer vacationLastYearUsed = 0;
	
	@Column(name="abs_fac_usate")
	public Integer vacationCurrentYearUsed = 0;
	
	@Column(name="asb_p_usati")
	public Integer permissionUsed = 0;
	
	@Column(name="abs_rc_usati")
	public Integer recoveryDayUsed = 0;		//numeroRiposiCompensativi
	
	/**************************************************************************
	 * FONTI DELL'ALGORITMO RESIDUI
	 **************************************************************************/
	
	@Column(name="s_r_bp")
	public int buoniPastoDalMesePrecedente = 0;
	
	@Column(name="s_bp_consegnati")
	public int buoniPastoConsegnatiNelMese = 0;
	
	@Column(name="s_bd_usati")
	public int buoniPastoUsatiNelMese = 0;
	
	@Column(name="s_r_ac_initmese")
	public int initResiduoAnnoCorrenteNelMese = 0;	//per il template (se sourceContract è del mese)
	
	@Column(name="s_r_ap")
	public int initMonteOreAnnoPassato = 0;		//dal precedente recap ma è utile salvarlo
	
	@Column(name="s_r_ac")
	public int initMonteOreAnnoCorrente = 0;	//dal precedente recap ma è utile salvarlo
	
	@Column(name="s_pf")
	public int progressivoFinaleMese = 0;	//person day	
	
	@Column(name="s_pfp")
	public int progressivoFinalePositivoMesePrint = 0;	//per il template

	@Column(name="s_r_ap_usabile")
	public boolean possibileUtilizzareResiduoAnnoPrecedente = true;
	
	@Column(name="s_s1")
	public int straordinariMinutiS1Print	 = 0;	//per il template

	@Column(name="s_s2")
	public int straordinariMinutiS2Print	 = 0;	//per il template
	
	@Column(name="s_s3")
	public int straordinariMinutiS3Print	 = 0;	//per il template
	
	@Column(name="s_rc_min")
	public int riposiCompensativiMinutiPrint = 0;	//per il template
	
	@Column(name="s_ol")
	public int oreLavorate = 0;				// riepilogo per il template
	
	/**************************************************************************
	 * DECISIONI DELL'ALGORITMO
	 **************************************************************************/

	@Column(name="d_pfn_ap")
	public int progressivoFinaleNegativoMeseImputatoAnnoPassato = 0;
	@Column(name="d_pfn_ac")
	public int progressivoFinaleNegativoMeseImputatoAnnoCorrente = 0;
	@Column(name="d_pfn_pfp")
	public int progressivoFinaleNegativoMeseImputatoProgressivoFinalePositivoMese = 0;
	
	@Column(name="d_rc_ap")
	public int riposiCompensativiMinutiImputatoAnnoPassato = 0;
	@Column(name="d_rc_ac")
	public int riposiCompensativiMinutiImputatoAnnoCorrente = 0;
	@Column(name="d_rc_pfp")
	public int riposiCompensativiMinutiImputatoProgressivoFinalePositivoMese = 0;
	
	@Column(name="d_r_ap")
	public Integer remainingMinutesLastYear = 0;
	
	@Column(name="d_r_ac")
	public Integer remainingMinutesCurrentYear = 0;
	
	@Column(name="d_r_bp")
	public Integer remainingMealTickets = 0; //buoniPastoResidui
	
	
	/**************************************************************************
	 * DI SUPPORTO (VALORIZZATI PER POI ESSERE IMPUTATI)
	 **************************************************************************/
	
	@Transient
	public int straordinariMinuti 			 = 0;	//competences (di appoggio deducibile dalle imputazioni)
	
	@Transient
	public int riposiCompensativiMinuti 	 = 0;	//absences  (di appoggio deducibile dalle imputazioni)	
													// in charts è usato... capire cosa contiene alla fine e fixare
	@Transient
	public int progressivoFinaleNegativoMese = 0;	//person day	// (di appoggio deducibile dalle imputazioni)
	
	/**************************************************************************
	 * DI SUPPORTO (VALORIZZATI PER POI ESSERE SCORPORATI)
	 **************************************************************************/
	
	@Transient
	public int progressivoFinalePositivoMese = 0;	//person day
													// forse è usato... capire cosa contiene alla fine e fixare

	/**************************************************************************
	 * TRANSIENTI DA METTERE NEL WRAPPER
	 **************************************************************************/
	
	@Transient public Person person;
	@Transient public String contractDescription;
	@Transient public ContractMonthRecap mesePrecedente;
	@Transient public int qualifica;
	@Transient public IWrapperContract wcontract;
	
	
}
