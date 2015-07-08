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

/* Romanian initialisation for the jQuery UI date picker plugin. */
/* Written by Edmond L. (ll_edmond@walla.com). */
jQuery(function($){
	$.datepicker.regional['ro'] = {
		clearText: 'Curat', clearStatus: 'Sterge data curenta',
		closeText: 'Inchide', closeStatus: 'Inchide fara schimbare',
		prevText: '&#x3c;Anterior', prevStatus: 'Arata luna trecuta',
		prevBigText: '&#x3c;&#x3c;', prevBigStatus: '',
		nextText: 'Urmator&#x3e;', nextStatus: 'Arata luna urmatoare',
		nextBigText: '&#x3e;&#x3e;', nextBigStatus: '',
		currentText: 'Azi', currentStatus: 'Arata luna curenta',
		monthNames: ['Ianuarie','Februarie','Martie','Aprilie','Mai','Junie',
		'Julie','August','Septembrie','Octobrie','Noiembrie','Decembrie'],
		monthNamesShort: ['Ian', 'Feb', 'Mar', 'Apr', 'Mai', 'Jun',
		'Jul', 'Aug', 'Sep', 'Oct', 'Noi', 'Dec'],
		monthStatus: 'Arata o luna diferita', yearStatus: 'Arat un an diferit',
		weekHeader: 'Sapt', weekStatus: 'Saptamana anului',
		dayNames: ['Duminica', 'Luni', 'Marti', 'Miercuri', 'Joi', 'Vineri', 'Sambata'],
		dayNamesShort: ['Dum', 'Lun', 'Mar', 'Mie', 'Joi', 'Vin', 'Sam'],
		dayNamesMin: ['Du','Lu','Ma','Mi','Jo','Vi','Sa'],
		dayStatus: 'Seteaza DD ca prima saptamana zi', dateStatus: 'Selecteaza D, M d',
		dateFormat: 'mm/dd/yy', firstDay: 0, 
		initStatus: 'Selecteaza o data', isRTL: false};
	$.datepicker.setDefaults($.datepicker.regional['ro']);
});
