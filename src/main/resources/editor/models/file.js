steal.then('jsonix')
.then(function($) {
	
	$.Model('Editor.File',
	/* @Static */
	{
		/**
		 * Returns the file that is located at the given path, the given id or the given id and language
		 * @param {Object} params path, id or id and langauge
		 */
		findOne: function(params, success, error) {
			if ('path' in params) {
				$.ajax('/system/weblounge/files?path=' + params.path, {
					success: function(data, textStatus, jqXHR) {
						// TODO eTag
//						var json = Editor.File.parseXML(data);
//						var file = Editor.File.wrap(json);
//						file.setETag(jqXHR.getResponseHeader('Etag'));
//						success(file);
						this.callback(['parseXML', 'wrap', success])
					}
				});
			} 
			else if ('language' in params) {
				$.ajax('/system/weblounge/files/' + params.id + '/content/' + params.language, {
					success: this.callback(['parseXML', 'wrap', success])
				});
			}
			else if ('id' in params) {
				$.ajax('/system/weblounge/files/' + params.id, {
					success: this.callback(['parseXML', 'wrap', success])
				});
			}
		},
		
		/**
		 * Get all Files
		 */
		findAll: function(params, success, error) {
			var url = "/system/weblounge/files/?sort=created-asc&limit=0&offset=0";
			if ('filter' in params) {
				url += "&filter=" + params.filter;
			}
			$.ajax(url, {
				success: function(xml) {
					var json = Editor.File.parseXML(xml);
					success(Editor.File.concatFiles(json));
				}
			});
		},
		
		/**
		 * Get Recent Files
		 */
		findRecent: function(params, success, error) {
			var url = "/system/weblounge/files/?sort=modified-desc&limit=8&offset=0";
			if ('filter' in params) {
				url += "&filter=" + params.filter;
			}
			$.ajax(url, {
				success: function(xml) {
					var json = Editor.File.parseXML(xml);
					success(Editor.File.concatFiles(json));
				}
			});
		},
		
		/**
		 * Get Pending Files
		 */
		findPending: function(params, success, error) {
			var url = "/system/weblounge/files/pending";
			if ('filter' in params) {
				url += "?filter=" + params.filter;
			}
			$.ajax(url, {
				success: function(xml) {
					var json = Editor.File.parseXML(xml);
					success(Editor.File.concatFiles(json));
				}
			});
		},
		
		/**
		 * Get files searched by string
		 */
		findBySearch: function(params, success, error) {
			var url = '/system/weblounge/files/?searchterms=' + params.search + '&sort=modified-desc&limit=8&offset=0';
			if ('filter' in params) {
				url += "&filter=" + params.filter;
			}
			$.ajax(url, {
				success: function(xml) {
					var json = Editor.File.parseXML(xml);
					success(Editor.File.concatFiles(json));
				}
			});
		},
		
		/**
		 * Updates the specified file or the specified file content.
		 * @param {Object} params The file identifier, language and eTag
		 * @param {File} file The file object
		 */
		update: function(params, file, success, error){
			var headers = {};
			var url = '';
			
			if('eTag' in params)
				headers = {"If-Match": params.eTag};
			if ('id' in params)
				url = '/system/weblounge/files/' + params.id;
			if ('language' in params)
				url += '/content/' + params.language;
			
			$.ajax({
				url: url,
				type: 'put',
				headers: headers,
				success: success,
				dataType: 'xml',
				data: {content : Editor.File.parseJSON(file)}
			});
		},
		
		/**
		 * Creates a new file, either at the given path or at a random location and returns the REST url of the created resource.
		 * @param {Object} params The target path
		 * @param {File} file The file data
		 */
		create: function(params, file, success, error){
			if ('path' in params) {
				$.ajax({
					url: '/system/weblounge/files/',
					type: 'post',
					dataType: 'xml',
					success: success,
					data: {path : params.path, content : Editor.File.parseJSON(file)}
				});
			}	
		},
		
		/**
		 * Creates a new file as well as content, either at the given path or at a random location and returns the REST url of the created resource.
		 * @param {Object} params The target path, mimeType and languageid
		 * @param {File} file The file data
		 */
		upload: function(params, file, success, error){
			if ('path' in params) {
				$.ajax({
					url: '/system/weblounge/files/uploads',
					type: 'post',
					dataType: 'xml',
					data: {path : params.path, mimeType: params.mimeType, languageid: params.languageid, content : Editor.File.parseJSON(file)}
				});
			}	
		},
		
		/**
		 * Deletes the specified file or the specified file content.
		 * @param {Object} params The file id or id and language
		 */
		destroy: function(params, success, error){
			if ('language' in params) {
				$.ajax({
					url: '/system/weblounge/files/' + params.id + '/content/' + params.language,
					type: 'delete',
					success: success
				});
			}
			else if ('id' in params) {
				$.ajax({
					url: '/system/weblounge/files/' + params.id,
					type: 'delete',
					success: success
				});
			}
		},
		
		/**
		 * Concat file and image array
		 */
		concatFiles: function(json) {
			if($.isEmptyObject(json.value.file)) {
				return json.value.image;
			}
			else {
				if($.isEmptyObject(json.value.image)) return json.value.file;
				return json.value.file.concat(json.value.image);
			}
		},
		
		/**
		 * Converts XML to JSON
		 */
		parseXML: function(xml) {
			var unmarshaller = Editor.Jsonix.context().createUnmarshaller();
			return unmarshaller.unmarshalDocument(xml);
		},
		
		/**
		 * Converts JSON to XML
		 */
		parseJSON: function(json) {
			var marshaller = Editor.Jsonix.context().createMarshaller();
			return marshaller.marshalString(json);
		}

	},
	/* @Prototype */
	{
		
		getMetadata: function(language) {
			var metadata = new Object();
			metadata.title = '';
			metadata.description = '';
			metadata.tags = [];
			metadata.author = '';
			
			if($.isEmptyObject(this.value.head.metadata)) {
				this.value.head.metadata = {};
			}
			if(!$.isEmptyObject(this.value.head.metadata.title) && this.value.head.metadata.title[language] != undefined) {
				metadata.title = this.value.head.metadata.title[language];
			}
			if(!$.isEmptyObject(this.value.head.metadata.description) && this.value.head.metadata.description[language] != undefined) {
				metadata.description = this.value.head.metadata.description[language];
			}
			if(!$.isEmptyObject(this.value.head.metadata.subject)) {
				metadata.description = this.value.head.metadata.subject;
			}
			if(!$.isEmptyObject(this.value.head.created)) {
				metadata.author = this.value.head.created.user.name;
			}
			return metadata;
		},
		
		saveMetadata: function(metadata, language, eTag) {
			if($.isEmptyObject(this.value.head.metadata)) {
				this.value.head.metadata = {};
			}
			if($.isEmptyObject(this.value.head.metadata.title)) {
				this.value.head.metadata.title = {};
			}
			if($.isEmptyObject(this.value.head.metadata.description)) {
				this.value.head.metadata.description = {};
			}
			if($.isEmptyObject(this.value.head.metadata.subject)) {
				this.value.head.metadata.subject = [];
			}
			
			this.value.head.metadata.title[language] = metadata.title;
			this.value.head.metadata.description[language] = metadata.description;
			
			// Filter out empty values
			if(!$.isEmptyObject(metadata.tags.split)) {
				this.value.head.metadata.subject = metadata.tags.split(/\s*,\s*/).filter(function(value) { 
					return value != ''; 
				});
			}
			
			this.value.head.created.user.name = metadata.author;
			if(eTag == null) 
				Editor.File.update({id:this.value.id}, this);
			else {
				eTag = eTag.replace(/"/g, '');
				Editor.File.update({id:this.value.id, eTag: eTag}, this);
			}
		}
		
	});

});
