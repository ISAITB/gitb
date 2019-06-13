# Provides references to and utilities for common objects that can be used accross different controllers.
# The intention is to use those common objects to bind them to different views. So,
# when a controller changes an attribute of an object (not the object itself!), the
# change will be reflected to a different view that is controlled by a different controller.
class DataService
	
	@$inject = ['Constants', '$q']
	constructor: (@Constants, @$q) ->
		@destroy() #we call destroy inside the constructor to create objects :)

	#should be called after logout, since no user data should be kept any more
	destroy: () ->
		@user = undefined
		@vendor = undefined
		@community = undefined
		@configuration = undefined
		@isSystemAdmin = false
		@isVendorUser = false
		@isCommunityAdmin = false
		@isDomainUser = false
		@acceptedEmailAttachmentTypes = undefined
		@searchState = undefined

	clearSearchState: () =>
		@searchState = undefined

	setSearchState: (searchState, origin) =>
		@searchState = {}
		@searchState.data = searchState
		@searchState.origin = origin

	setUser: (user) ->
		@user = user

		@isVendorAdmin = (@user.role == @Constants.USER_ROLE.VENDOR_ADMIN)
		@isVendorUser  = (@user.role == @Constants.USER_ROLE.VENDOR_USER)
		@isDomainUser  = (@user.role == @Constants.USER_DOMAIN_USER)
		@isSystemAdmin = (@user.role == @Constants.USER_ROLE.SYSTEM_ADMIN)
		@isCommunityAdmin = (@user.role == @Constants.USER_ROLE.COMMUNITY_ADMIN)

	setConfiguration: (config) ->
		@configuration = config
		@acceptedEmailAttachmentTypes = {}
		acceptedTypes = config['email.attachments.allowedTypes'].split(',')
		for acceptedType in acceptedTypes
			@acceptedEmailAttachmentTypes[acceptedType] = true

	setVendor: (vendor) ->
		@vendor = vendor

	setCommunity: (community) ->
		@community = community

	setTestsToExecute: (tests) ->
		@tests = tests

	getFileInfo: (blob, filename) =>
		job = @$q.defer()	
		fileReader = new FileReader()
		fileReader.onloadend = (e) =>
			byteArray = new Uint8Array(e.target.result)
			if byteArray.length >= 8
				arr = (new Uint8Array(e.target.result)).subarray(0, 8)
			else if byteArray.length >= 4
				arr = (new Uint8Array(e.target.result)).subarray(0, 4)
			header = ""
			for i in [0..arr.length-1]
				header += arr[i].toString(16)
			if header.startsWith('89504e47')
				type = "image/png"
				extension = "png"
			else if header.startsWith('47494638')
				type = "image/gif"
				extension = "gif"
			else if header.startsWith('ffd8ffe0') || header.startsWith('ffd8ffe1') || header.startsWith('ffd8ffe2') || header.startsWith('ffd8ffe3') || header.startsWith('ffd8ffe8')
				type = "image/jpeg"
				extension = "jpeg"
			else if header.startsWith('49492a00') || header.startsWith('4d4d002d')
				type = "image/tiff"
				extension = "tiff"
			else if header.startsWith('25504446')
				type = "application/pdf"
				extension = "pdf"
			else if header.startsWith('504b0304') || header.startsWith('504b0506') || header.startsWith('504b0708')
				type = "application/zip"
				extension = "zip"
			else if header.startsWith('efbbbf') || header.startsWith('fffe')
				type = "text/plain"
				extension = "txt"
			else if header.startsWith('3c3f786d6c20')
				type = "text/xml"
				extension = "xml"
			else
				undefined

			if !filename?
				filename = "file"
			if extension?
				filename += '.'+extension

			info = {
				type: type
				extension: extension
				filename: filename
			}
			job.resolve(info)

		fileReader.readAsArrayBuffer(blob);
		job.promise

	anyContentToBlob: (anyContent, contentType) =>
		if anyContent.embeddingMethod == 'BASE64'
			bb = @b64toBlob(anyContent.value, contentType)
		else
			if contentType?
				bb = new Blob([anyContent.value], contentType)
			else 
				bb = new Blob([anyContent.value])

	base64FromDataURL: (dataURL) =>
		dataURL.substring(dataURL.indexOf(',')+1)

	mimeTypeFromDataURL: (dataURL) =>
		dataURL.substring(dataURL.indexOf(':')+1, dataURL.indexOf(';'))

	extensionFromMimeType: (mimeType) =>
		if mimeType == "text/xml" || mimeType == "application/xml"
			".xml"
		else if mimeType == "application/zip" || mimeType == "application/x-zip-compressed"
			".zip"
		else if mimeType == "application/pkix-cert"
			".cer"
		else if mimeType == "application/pdf"
			".pdf"
		else if mimeType == "text/plain"
			".txt"
		else if mimeType == "image/png"
			".png"
		else if mimeType == "image/gif"
			".gif"
		else if mimeType == "image/gif"
			".gif"
		else if mimeType == "image/jpeg"
			".jpeg"
		else
			""

	b64toBlob: (b64Data, contentType, sliceSize) =>
		contentType = contentType || ''
		sliceSize = sliceSize || 512;
		byteCharacters = atob(b64Data)
		byteArrays = []
		offset = 0
		while offset < byteCharacters.length
			slice = byteCharacters.slice(offset, offset + sliceSize)
			byteNumbers = new Array(slice.length)
			for i in [0...slice.length]
				byteNumbers[i] = slice.charCodeAt(i)
			byteArray = new Uint8Array(byteNumbers)
			byteArrays.push(byteArray);
			offset += sliceSize
		if contentType?
			blob = new Blob(byteArrays, {type: contentType})
		else
			blob = new Blob(byteArrays)

	testStatusText: (completedCount, failedCount, undefinedCount) =>
		totalCount = completedCount + failedCount + undefinedCount
		resultText = completedCount + ' of ' + totalCount + ' passed'
		if totalCount > completedCount
			resultText += ' ('
			if failedCount > 0
				resultText += failedCount + ' failed'
				if undefinedCount > 0
					resultText += ', '
			if undefinedCount > 0
				resultText += undefinedCount + ' undefined'
			resultText += ')'
		resultText

	asCsvString: (text) =>
		textStr = ''
		if text?
			textStr = String(text)
			if textStr.length > 0
				# Replace values that can break the CSV format
				textStr = textStr.replace(/(,|\s+)/g, ' ')
				# Prevent CSV formula injection attacks
				charsToEscape = ['=','@','+','-']
				if charsToEscape.indexOf(textStr.charAt(0)) != -1
					textStr = '\'' + textStr
		textStr

	
	exportAllAsCsv: (header, data) =>
		if data.length > 0
			csv = header.toString() + '\n'
			for o, i in data
				line = ''
				idx = 0
				for k, v of o
					if idx++ != 0
						line += ','
					line += @asCsvString(v)
				csv += if i < data.length then line + '\n' else line
			blobData = new Blob([csv], {type: 'text/csv'});
			saveAs(blobData, 'export.csv');

	exportPropertiesAsCsv: (header, columnMap, data) =>
		if data.length > 0
			csv = header.toString() + '\n'
			for rowData, rowIndex in data
				line = ''
				for columnName, columnIndex in columnMap
					if columnIndex != 0
						line += ','
					line += @asCsvString(rowData[columnName])
				csv += if rowIndex < data.length then line + '\n' else line
			blobData = new Blob([csv], {type: 'text/csv'});
			saveAs(blobData, 'export.csv');



services.service('DataService', DataService)
