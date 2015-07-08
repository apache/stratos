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

editAreaLoader.load_syntax["robotstxt"] = {
	'DISPLAY_NAME' : 'Robots txt',
	'COMMENT_SINGLE' : {1 : '#'},
	'COMMENT_MULTI' : {},
	'QUOTEMARKS' : [],
	'KEYWORD_CASE_SENSITIVE' : false,
	'KEYWORDS' : {
		'attributes' : ['User-agent', 'Disallow', 'Allow', 'Crawl-delay'],
		'values' : ['*'],
		'specials' : ['*']
	},
	'OPERATORS' :[':'],
	'DELIMITERS' :[],
	'STYLES' : {
		'COMMENTS': 'color: #AAAAAA;',
		'QUOTESMARKS': 'color: #6381F8;',
		'KEYWORDS' : {
			'attributes' : 'color: #48BDDF;',
			'values' : 'color: #2B60FF;',
			'specials' : 'color: #FF0000;'
			},
	'OPERATORS' : 'color: #FF00FF;',
	'DELIMITERS' : 'color: #60CA00;'			
	}
};
