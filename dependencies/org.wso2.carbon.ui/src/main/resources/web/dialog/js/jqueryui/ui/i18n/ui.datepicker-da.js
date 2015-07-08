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

/* Danish initialisation for the jQuery UI date picker plugin. */
/* Written by Jan Christensen ( deletestuff@gmail.com). */
jQuery(function($){
    $.datepicker.regional['da'] = {
		clearText: 'Nulstil', clearStatus: 'Nulstil den aktuelle dato',
		closeText: 'Luk', closeStatus: 'Luk uden ændringer',
        prevText: '&#x3c;Forrige', prevStatus: 'Vis forrige måned',
		prevBigText: '&#x3c;&#x3c;', prevBigStatus: '',
		nextText: 'Næste&#x3e;', nextStatus: 'Vis næste måned',
		nextBigText: '&#x3e;&#x3e;', nextBigStatus: '',
		currentText: 'Idag', currentStatus: 'Vis aktuel måned',
        monthNames: ['Januar','Februar','Marts','April','Maj','Juni', 
        'Juli','August','September','Oktober','November','December'],
        monthNamesShort: ['Jan','Feb','Mar','Apr','Maj','Jun', 
        'Jul','Aug','Sep','Okt','Nov','Dec'],
		monthStatus: 'Vis en anden måned', yearStatus: 'Vis et andet år',
		weekHeader: 'Uge', weekStatus: 'Årets uge',
		dayNames: ['Søndag','Mandag','Tirsdag','Onsdag','Torsdag','Fredag','Lørdag'],
		dayNamesShort: ['Søn','Man','Tir','Ons','Tor','Fre','Lør'],
		dayNamesMin: ['Sø','Ma','Ti','On','To','Fr','Lø'],
		dayStatus: 'Sæt DD som første ugedag', dateStatus: 'Vælg D, M d',
        dateFormat: 'dd-mm-yy', firstDay: 0, 
		initStatus: 'Vælg en dato', isRTL: false};
    $.datepicker.setDefaults($.datepicker.regional['da']); 
});
