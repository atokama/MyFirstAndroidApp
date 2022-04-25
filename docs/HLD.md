### HLD

Image processing done by a chain of filters. Filters might hold memory for the processing result, or
do processing in-place. Filters have common set of Views and Controls for things like mean execution
time, enabling/disabling filter, etc. Filters might have custom Views or Controls for their specific
needs, like change threshold or display. User can separately select filter to display and a filter
to control.