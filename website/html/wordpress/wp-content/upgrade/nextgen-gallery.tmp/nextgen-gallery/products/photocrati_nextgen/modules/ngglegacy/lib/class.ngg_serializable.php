<?php

class Ngg_Serializable
{
	/**
	 * Serializes the data
	 * @param mixed $value
	 * @return string
	 */
	function serialize($value)
	{
		//Using json_encode here because PHP's serialize is not Unicode safe
		return base64_encode(json_encode($value));
	}


	/**
	 * Unserializes data using our proprietary format
	 * @param string $value
	 * @return mixed
	 */
	function unserialize($value)
	{
		$retval = NULL;
		if (is_string($value))
		{
			$retval = stripcslashes($value);

			if (strlen($value) > 1)
			{
                // We can't always rely on base64_decode() or json_decode() to return FALSE as their documentation
                // claims so check if $retval begins with a: as that indicates we have a serialized PHP object.
                if (strpos($retval, 'a:') === 0)
                {
                    $er = error_reporting(0);
                    $retval = unserialize($value);
                    error_reporting($er);
                }
                else {
                    // We use json_decode() here because PHP's unserialize() is not Unicode safe.
                    $retval = json_decode(base64_decode($retval), TRUE);
                }
			}
		}

		return $retval;
	}
}
