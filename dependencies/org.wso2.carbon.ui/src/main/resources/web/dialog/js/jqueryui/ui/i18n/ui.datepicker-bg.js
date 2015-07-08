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

/* Bulgarian initialisation for the jQuery UI date picker plugin. */
/* Written by Stoyan Kyosev (http://svest.org). */
jQuery(function($){
    $.datepicker.regional['bg'] = {
		clearText: 'изчисти', clearStatus: 'изчисти актуалната дата',
        closeText: 'затвори', closeStatus: 'затвори без промени',
        prevText: '&#x3c;назад', prevStatus: 'покажи последния месец',
		prevBigText: '&#x3c;&#x3c;', prevBigStatus: '',
        nextText: 'напред&#x3e;', nextStatus: 'покажи следващия месец',
		nextBigText: '&#x3e;&#x3e;', nextBigStatus: '',
        currentText: 'днес', currentStatus: '',
        monthNames: ['Януари','Февруари','Март','Април','Май','Юни',
        'Юли','Август','Септември','Октомври','Ноември','Декември'],
        monthNamesShort: ['Яну','Фев','Мар','Апр','Май','Юни',
        'Юли','Авг','Сеп','Окт','Нов','Дек'],
        monthStatus: 'покажи друг месец', yearStatus: 'покажи друга година',
        weekHeader: 'Wk', weekStatus: 'седмица от месеца',
        dayNames: ['Неделя','Понеделник','Вторник','Сряда','Четвъртък','Петък','Събота'],
        dayNamesShort: ['Нед','Пон','Вто','Сря','Чет','Пет','Съб'],
        dayNamesMin: ['Не','По','Вт','Ср','Че','Пе','Съ'],
        dayStatus: 'Сложи DD като първи ден от седмицата', dateStatus: 'Избери D, M d',
        dateFormat: 'dd.mm.yy', firstDay: 1,
        initStatus: 'Избери дата', isRTL: false};
    $.datepicker.setDefaults($.datepicker.regional['bg']);
});
