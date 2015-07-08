/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/* Dutch (UTF-8) initialisation for the jQuery UI date picker plugin. */
/* Written by ??? */
jQuery(function($){
	$.datepicker.regional['nl'] = {
		clearText: 'Wissen', clearStatus: 'Wis de huidige datum',
		closeText: 'Sluiten', closeStatus: 'Sluit zonder verandering',
		prevText: '&#x3c;Terug', prevStatus: 'Laat de voorgaande maand zien',
		prevBigText: '&#x3c;&#x3c;', prevBigStatus: '',
		nextText: 'Volgende&#x3e;', nextStatus: 'Laat de volgende maand zien',
		nextBigText: '&#x3e;&#x3e;', nextBigStatus: '',
		currentText: 'Vandaag', currentStatus: 'Laat de huidige maand zien',
		monthNames: ['Januari','Februari','Maart','April','Mei','Juni',
		'Juli','Augustus','September','Oktober','November','December'],
		monthNamesShort: ['Jan','Feb','Mrt','Apr','Mei','Jun',
		'Jul','Aug','Sep','Okt','Nov','Dec'],
		monthStatus: 'Laat een andere maand zien', yearStatus: 'Laat een ander jaar zien',
		weekHeader: 'Wk', weekStatus: 'Week van het jaar',
		dayNames: ['Zondag','Maandag','Dinsdag','Woensdag','Donderdag','Vrijdag','Zaterdag'],
		dayNamesShort: ['Zon','Maa','Din','Woe','Don','Vri','Zat'],
		dayNamesMin: ['Zo','Ma','Di','Wo','Do','Vr','Za'],
		dayStatus: 'DD', dateStatus: 'D, M d',
		dateFormat: 'dd.mm.yy', firstDay: 1, 
		initStatus: 'Kies een datum', isRTL: false};
	$.datepicker.setDefaults($.datepicker.regional['nl']);
});