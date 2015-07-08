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

/* Esperanto initialisation for the jQuery UI date picker plugin. */
/* Written by Olivier M. (olivierweb@ifrance.com). */
jQuery(function($){
	$.datepicker.regional['eo'] = {
		clearText: 'Vakigi', clearStatus: '',
		closeText: 'Fermi', closeStatus: 'Fermi sen modifi',
		prevText: '&lt;Anta', prevStatus: 'Vidi la antaŭan monaton',
		prevBigText: '&#x3c;&#x3c;', prevBigStatus: '',
		nextText: 'Sekv&gt;', nextStatus: 'Vidi la sekvan monaton',
		nextBigText: '&#x3e;&#x3e;', nextBigStatus: '',
		currentText: 'Nuna', currentStatus: 'Vidi la nunan monaton',
		monthNames: ['Januaro','Februaro','Marto','Aprilo','Majo','Junio',
		'Julio','Aŭgusto','Septembro','Oktobro','Novembro','Decembro'],
		monthNamesShort: ['Jan','Feb','Mar','Apr','Maj','Jun',
		'Jul','Aŭg','Sep','Okt','Nov','Dec'],
		monthStatus: 'Vidi alian monaton', yearStatus: 'Vidi alian jaron',
		weekHeader: 'Sb', weekStatus: '',
		dayNames: ['Dimanĉo','Lundo','Mardo','Merkredo','Ĵaŭdo','Vendredo','Sabato'],
		dayNamesShort: ['Dim','Lun','Mar','Mer','Ĵaŭ','Ven','Sab'],
		dayNamesMin: ['Di','Lu','Ma','Me','Ĵa','Ve','Sa'],
		dayStatus: 'Uzi DD kiel unua tago de la semajno', dateStatus: 'Elekti DD, MM d',
		dateFormat: 'dd/mm/yy', firstDay: 0,
		initStatus: 'Elekti la daton', isRTL: false};
	$.datepicker.setDefaults($.datepicker.regional['eo']);
});
