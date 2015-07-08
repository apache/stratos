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

/* Polish initialisation for the jQuery UI date picker plugin. */
/* Written by Jacek Wysocki (jacek.wysocki@gmail.com). */
jQuery(function($){
	$.datepicker.regional['pl'] = {
		clearText: 'Wyczyść', clearStatus: 'Wyczyść obecną datę',
		closeText: 'Zamknij', closeStatus: 'Zamknij bez zapisywania',
		prevText: '&#x3c;Poprzedni', prevStatus: 'Pokaż poprzedni miesiąc',
		prevBigText: '&#x3c;&#x3c;', prevBigStatus: '',
		nextText: 'Następny&#x3e;', nextStatus: 'Pokaż następny miesiąc',
		nextBigText: '&#x3e;&#x3e;', nextBigStatus: '',
		currentText: 'Dziś', currentStatus: 'Pokaż aktualny miesiąc',
		monthNames: ['Styczeń','Luty','Marzec','Kwiecień','Maj','Czerwiec',
		'Lipiec','Sierpień','Wrzesień','Październik','Listopad','Grudzień'],
		monthNamesShort: ['Sty','Lu','Mar','Kw','Maj','Cze',
		'Lip','Sie','Wrz','Pa','Lis','Gru'],
		monthStatus: 'Pokaż inny miesiąc', yearStatus: 'Pokaż inny rok',
		weekHeader: 'Tydz', weekStatus: 'Tydzień roku',
		dayNames: ['Niedziela','Poniedzialek','Wtorek','Środa','Czwartek','Piątek','Sobota'],
		dayNamesShort: ['Nie','Pn','Wt','Śr','Czw','Pt','So'],
		dayNamesMin: ['N','Pn','Wt','Śr','Cz','Pt','So'],
		dayStatus: 'Ustaw DD jako pierwszy dzień tygodnia', dateStatus: 'Wybierz D, M d',
		dateFormat: 'yy-mm-dd', firstDay: 1, 
		initStatus: 'Wybierz datę', isRTL: false};
	$.datepicker.setDefaults($.datepicker.regional['pl']);
});
