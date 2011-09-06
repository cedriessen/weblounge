$.Controller("Editor.Author",
/* @static */
{
},

/* @prototype */
{
    /**
     * Initialize a new AuthorEditor controller.
     */
    init: function(el) {
    	var year = this.element.find('input[name="property:year"]');
    	var login = this.element.find('input[name="property:login"]');
    	var name = this.element.find('input[name="property:name"]');
    	
    	if(year.val() == "") {
    		year.val(new Date().getFullYear());
    	}
    	
    	Runtime.findOne({}, function(runtime) {
    		if(login.val() == "") {
    			login.val(runtime.getUserLogin());
    		}
    		if(name.val() == "") {
    			name.val(runtime.getUserName());
    		}
    	});
    	
    }
    
});

$('#wbl-pageleteditor form').editor_author();